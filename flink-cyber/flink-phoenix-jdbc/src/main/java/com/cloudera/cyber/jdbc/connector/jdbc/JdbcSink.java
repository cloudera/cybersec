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

package com.cloudera.cyber.jdbc.connector.jdbc;

import com.cloudera.cyber.jdbc.connector.jdbc.internal.GenericJdbcSinkFunction;
import com.cloudera.cyber.jdbc.connector.jdbc.internal.JdbcOutputFormat;
import com.cloudera.cyber.jdbc.connector.jdbc.internal.JdbcStatementBuilder;
import com.cloudera.cyber.jdbc.connector.jdbc.internal.connection.PhoenixJdbcConnectionProvider;
import com.cloudera.cyber.jdbc.connector.jdbc.internal.executor.JdbcBatchStatementExecutor;
import java.util.function.Function;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

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
