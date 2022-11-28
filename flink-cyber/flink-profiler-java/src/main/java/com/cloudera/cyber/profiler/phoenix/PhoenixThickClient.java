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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class PhoenixThickClient {
    //jdbc:phoenix [ :<zookeeper quorum> [ :<port number> [ :<root node> [ :<principal> [ :<keytab file> ] ] ] ] ]
    public final String dbUrl;
    public final String userName;
    public final String password;

    public PhoenixThickClient(String userName, String password, String dbUrl) {
        this.userName = userName;
        this.password = password;
        this.dbUrl = dbUrl;
    }

    public ResultSetMetaData getTableMetadata(String sql) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password)) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                return ps.getMetaData();
            }
        } catch (SQLException e) {
            log.error("Execute SQL State: {}\n{}", e.getSQLState(), e.getMessage());
            throw e;
        }
    }

    public void executeSql(String sql) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password)) {
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
        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password)) {
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                consumer.accept(ps);
                ps.executeUpdate(sql);
                conn.commit();
            }
        } catch (SQLException e) {
            log.error("Insert SQL State: {}\n{}", e.getSQLState(), e.getMessage());
            throw e;
        }

    }

    public <T> List<T> selectListResultWithParams(String sql, Function<ResultSet, T> mapper, Consumer<PreparedStatement> consumer) throws SQLException {
        List<T> results = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password)) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                consumer.accept(ps);
                try (ResultSet resultSet = ps.executeQuery(sql)) {
                    while (resultSet.next()) {
                        results.add(mapper.apply(resultSet));
                    }
                }
            }
            conn.commit();
            return results;
        } catch (SQLException e) {
            log.error("Select list SQL State: {}\n{}", e.getSQLState(), e.getMessage());
            throw e;
        }
    }

    public <T> T selectResultWithParams(String sql, Function<ResultSet, T> mapper, Consumer<PreparedStatement> consumer) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl, userName, password)) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                consumer.accept(ps);
                try(ResultSet resultSet = ps.executeQuery(sql)) {
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
}
