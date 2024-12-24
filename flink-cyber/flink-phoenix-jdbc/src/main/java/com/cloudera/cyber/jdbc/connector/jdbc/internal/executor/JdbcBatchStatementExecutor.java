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
