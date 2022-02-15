package com.cloudera.cyber.jdbc.connector.jdbc.internal;

import org.apache.flink.util.function.BiConsumerWithException;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface JdbcStatementBuilder<T>
        extends BiConsumerWithException<PreparedStatement, T, SQLException>, Serializable {}
