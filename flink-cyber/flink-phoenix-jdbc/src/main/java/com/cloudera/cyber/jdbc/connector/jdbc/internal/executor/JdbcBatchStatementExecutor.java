package com.cloudera.cyber.jdbc.connector.jdbc.internal.executor;


import com.cloudera.cyber.jdbc.connector.jdbc.internal.JdbcStatementBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;


public interface JdbcBatchStatementExecutor<T> {

    void prepareStatements(Connection connection) throws SQLException;

    void addToBatch(T rec) throws SQLException;

    void executeBatch() throws SQLException;

    void closeStatements() throws SQLException;

    static <T, V> JdbcBatchStatementExecutor<T> simple(
            String sql, JdbcStatementBuilder<V> paramSetter, Function<T, V> valueTransformer) {
        return new SimpleBatchStatementExecutor<>(sql, paramSetter, valueTransformer);
    }
}
