/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.profiler.phoenix;

import com.cloudera.cyber.flink.Utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;

@Slf4j
public class PhoenixThinClient {

    private static final String DRIVER = "org.apache.phoenix.queryserver.client.Driver";
    public final String dbUrl;
    public final String userName;
    public final String password;

    public static final String PHOENIX_THIN_PROPERTY_URL = "phoenix.db.thin.url";
    public static final String PHOENIX_THIN_PROPERTY_SERIALIZATION = "phoenix.db.thin.serialization";
    public static final String PHOENIX_THIN_PROPERTY_AUTHENTICATION = "phoenix.db.thin.authentication";
    public static final String PHOENIX_THIN_PROPERTY_AVATICA_USER = "phoenix.db.thin.avatica_user";
    public static final String PHOENIX_THIN_PROPERTY_AVATICA_PASSWORD = "phoenix.db.thin.avatica_password";
    public static final String PHOENIX_THIN_PROPERTY_TRUSTSTORE = "phoenix.db.thin.truststore";
    public static final String PHOENIX_THIN_PROPERTY_TRUSTSTORE_PASSWORD = "phoenix.db.thin.truststore_password";
    public static final String PHOENIX_THIN_PROPERTY_PRINCIPAL = "phoenix.db.thin.principal";
    public static final String PHOENIX_THIN_PROPERTY_KEYTAB = "phoenix.db.thin.keytab";

    public PhoenixThinClient(ParameterTool params) {
        this.userName = params.get(PHOENIX_THIN_PROPERTY_AVATICA_USER);
        this.password = params.get(PHOENIX_THIN_PROPERTY_AVATICA_PASSWORD);
        dbUrl = "jdbc:phoenix:thin:url="
                + params.get(PHOENIX_THIN_PROPERTY_URL) + ";"
                + Optional.ofNullable(params.get(PHOENIX_THIN_PROPERTY_SERIALIZATION))
                        .map(str -> String.format("serialization=%s;", str)).orElse("serialization=PROTOBUF;")
                + Optional.ofNullable(params.get(PHOENIX_THIN_PROPERTY_AUTHENTICATION))
                        .map(str -> String.format("authentication=%s;", str)).orElse("authentication=BASIC;")
                + Optional.ofNullable(userName).map(str -> String.format("avatica_user=%s;", str)).orElse("")
                + Optional.ofNullable(password).map(str -> String.format("avatica_password=%s;", str)).orElse("")
                + Optional.ofNullable(params.get(PHOENIX_THIN_PROPERTY_PRINCIPAL))
                        .map(str -> String.format("principal=%s;", str)).orElse("")
                + Optional.ofNullable(params.get(PHOENIX_THIN_PROPERTY_KEYTAB))
                        .map(str -> String.format("keytab=%s;", str)).orElse("")
                + Optional.ofNullable(params.get(PHOENIX_THIN_PROPERTY_TRUSTSTORE))
                        .map(str -> String.format("truststore=%s;", str)).orElse("")
                + Optional.ofNullable(params.get(PHOENIX_THIN_PROPERTY_TRUSTSTORE_PASSWORD))
                        .map(str -> String.format("truststore_password=%s;", str)).orElse("");
    }

    private Object connectionResultMetaData(Function<Connection, Object> function) {
        try {
            Class.forName(DRIVER);
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                return function.apply(conn);
            } catch (SQLException e) {
                log.error("Connection exception SQL State: {}\n{}", e.getSQLState(), e.getMessage());
            }
        } catch (ClassNotFoundException e) {
            log.error("Driver class not found {}", DRIVER);
        }
        return null;
    }

    public ResultSetMetaData getTableMetadata(String sql) {
        return (ResultSetMetaData) connectionResultMetaData(conn -> executePreparedStatement(sql, conn));
    }

    private ResultSetMetaData executePreparedStatement(String sql, Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.getMetaData();
        } catch (SQLException e) {
            log.error("Execute SQL State: {}\n{}", e.getSQLState(), e.getMessage());
        }
        return null;
    }

    public void executeSql(String sql) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.execute();
            }
            conn.commit();
        } catch (SQLException e) {
            log.error("Execute SQL State: {}\n{}", e.getSQLState(), e.getMessage());
            throw e;
        }
    }

    public void insertIntoTable(String sql, Consumer<PreparedStatement> consumer) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                consumer.accept(ps);
                ps.executeUpdate();
                conn.commit();
            }
        } catch (SQLException e) {
            log.error("Insert SQL State: {}\n{}", e.getSQLState(), e.getMessage());
            throw e;
        }

    }

    public <T> List<T> selectListResult(String sql, Function<ResultSet, T> mapper) throws SQLException {
        return selectListResult(sql, mapper, null);
    }

    public <T> List<T> selectListResult(String sql, Function<ResultSet, T> mapper, Consumer<PreparedStatement> consumer)
          throws SQLException {
        List<T> results = new ArrayList<>();
        Optional<Consumer<PreparedStatement>> optionalConsumer = Optional.ofNullable(consumer);
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                optionalConsumer.ifPresent(con -> con.accept(ps));
                try (ResultSet resultSet = ps.executeQuery()) {
                    while (resultSet.next()) {
                        results.add(mapper.apply(resultSet));
                    }
                }
            }
            return results;
        } catch (SQLException e) {
            log.error("Select list SQL State: {}\n{}", e.getSQLState(), e.getMessage());
            throw e;
        }
    }

    public <T> T selectResult(String sql, Function<ResultSet, T> mapper) throws SQLException {
        return selectResult(sql, mapper, null);
    }

    public <T> T selectResult(String sql, Function<ResultSet, T> mapper, Consumer<PreparedStatement> consumer)
          throws SQLException {
        Optional<Consumer<PreparedStatement>> optionalConsumer = Optional.ofNullable(consumer);
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                optionalConsumer.ifPresent(con -> con.accept(ps));
                try (ResultSet resultSet = ps.executeQuery()) {
                    if (resultSet.next()) {
                        return mapper.apply(resultSet);
                    }
                }
            }
            return null;
        } catch (SQLException e) {
            log.error("Select SQL State: {}\n{}", e.getSQLState(), e.getMessage());
            throw e;
        }
    }

    public void executeSqlFromFile(String pathToFile) throws SQLException {
        executeSql(Utils.readResourceFile(pathToFile, getClass()));
    }

    public static String getDRIVER() {
        return DRIVER;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getUserName() {
        return userName;
    }

    public String getPassword() {
        return password;
    }
}
