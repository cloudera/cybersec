package com.cloudera.cyber.jdbc.connector.jdbc;

import com.cloudera.cyber.jdbc.connector.jdbc.internal.GenericJdbcSinkFunction;
import com.cloudera.cyber.jdbc.connector.jdbc.internal.JdbcOutputFormat;
import com.cloudera.cyber.jdbc.connector.jdbc.internal.JdbcStatementBuilder;
import com.cloudera.cyber.jdbc.connector.jdbc.internal.connection.PhoenixJdbcConnectionProvider;
import com.cloudera.cyber.jdbc.connector.jdbc.internal.executor.JdbcBatchStatementExecutor;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.util.function.Function;

public class JdbcSink {
    private JdbcSink() {
    }

    public static <T> SinkFunction<T> sink(
            String sql,
            JdbcStatementBuilder<T> statementBuilder,
            JdbcConnectionOptions connectionOptions) {
        return sink(sql, statementBuilder, JdbcExecutionOptions.defaults(), connectionOptions);
    }

    public static <T> SinkFunction<T> sink(
            String sql,
            JdbcStatementBuilder<T> statementBuilder,
            JdbcExecutionOptions executionOptions,
            JdbcConnectionOptions connectionOptions) {
        return new GenericJdbcSinkFunction<>(
                new JdbcOutputFormat<>(
                        new PhoenixJdbcConnectionProvider(connectionOptions),
                        executionOptions,
                        context ->
                                JdbcBatchStatementExecutor.simple(
                                        sql, statementBuilder, Function.identity()),
                        JdbcOutputFormat.RecordExtractor.identity()));
    }

}
