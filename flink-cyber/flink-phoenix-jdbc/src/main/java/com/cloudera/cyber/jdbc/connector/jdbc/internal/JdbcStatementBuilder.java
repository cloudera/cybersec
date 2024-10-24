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

package com.cloudera.cyber.jdbc.connector.jdbc.internal;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.apache.flink.util.function.BiConsumerWithException;

public interface JdbcStatementBuilder<T>
      extends BiConsumerWithException<PreparedStatement, T, SQLException>, Serializable {
}
