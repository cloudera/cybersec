package com.cloudera.cyber.profiler.phoenix;

import com.cloudera.cyber.flink.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.avatica.AvaticaPreparedStatement;
import org.apache.flink.api.java.utils.ParameterTool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class PhoenixThinClient {
    private static final String DRIVER = "org.apache.phoenix.queryserver.client.Driver";
    public final String dbUrl;
    public final String userName;
    public final String password;

    private static final String PHOENIX_THIN_PROPERTY_URL = "phoenix.db.thin.url";
    private static final String PHOENIX_THIN_PROPERTY_SERIALIZATION = "phoenix.db.thin.serialization";
    private static final String PHOENIX_THIN_PROPERTY_AUTHENTICATION = "phoenix.db.thin.authentication";
    private static final String PHOENIX_THIN_PROPERTY_AVATICA_USER = "phoenix.db.thin.avatica_user";
    private static final String PHOENIX_THIN_PROPERTY_AVATICA_PASSWORD = "phoenix.db.thin.avatica_password";
    private static final String PHOENIX_THIN_PROPERTY_TRUSTSTORE = "phoenix.db.thin.truststore";
    private static final String PHOENIX_THIN_PROPERTY_TRUSTSTORE_PASSWORD = "phoenix.db.thin.truststore_password";

    public PhoenixThinClient(ParameterTool params) {
        this.userName = params.get(PHOENIX_THIN_PROPERTY_AVATICA_USER);
        this.password = params.get(PHOENIX_THIN_PROPERTY_AVATICA_PASSWORD);
        dbUrl = new StringBuilder("jdbc:phoenix:thin:url=")
                .append(params.get(PHOENIX_THIN_PROPERTY_URL)).append(";")
                .append(Optional.ofNullable(params.get(PHOENIX_THIN_PROPERTY_SERIALIZATION)).map(str -> String.format("serialization=%s;", str)).orElse("serialization=PROTOBUF;"))
                .append(Optional.ofNullable(params.get(PHOENIX_THIN_PROPERTY_AUTHENTICATION)).map(str -> String.format("authentication=%s;", str)).orElse("authentication=BASIC;"))
                .append(Optional.ofNullable(userName).map(str -> String.format("avatica_user=%s;", str)).orElse(""))
                .append(Optional.ofNullable(password).map(str -> String.format("avatica_password=%s;", str)).orElse(""))
                .append(Optional.ofNullable(params.get(PHOENIX_THIN_PROPERTY_TRUSTSTORE)).map(str -> String.format("truststore=%s;", str)).orElse(""))
                .append(Optional.ofNullable(params.get(PHOENIX_THIN_PROPERTY_TRUSTSTORE_PASSWORD)).map(str -> String.format("truststore_password=%s;", str)).orElse(""))
                .toString();
    }

    private Object connectionResultMetaData(Function<Connection, Object> function) {
        try {
            Class.forName(DRIVER);
            try (Connection conn = DriverManager.getConnection(dbUrl)) {
                conn.setAutoCommit(true);
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
            conn.setAutoCommit(true);
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
            conn.setAutoCommit(true);
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

    public <T> List<T> selectListResultWithParams(String sql, Function<ResultSet, T> mapper, Consumer<PreparedStatement> consumer) throws SQLException {
        List<T> results = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(true);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                consumer.accept(ps);
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

    public <T> T selectResultWithParams(String sql, Function<ResultSet, T> mapper, Consumer<PreparedStatement> consumer) throws SQLException {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            conn.setAutoCommit(true);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                consumer.accept(ps);
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
