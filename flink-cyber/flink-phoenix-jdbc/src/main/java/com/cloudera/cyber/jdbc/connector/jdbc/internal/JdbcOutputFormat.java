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

import static org.apache.flink.util.Preconditions.checkNotNull;

import com.cloudera.cyber.jdbc.connector.jdbc.JdbcExecutionOptions;
import com.cloudera.cyber.jdbc.connector.jdbc.internal.connection.JdbcConnectionProvider;
import com.cloudera.cyber.jdbc.connector.jdbc.internal.executor.JdbcBatchStatementExecutor;
import java.io.Flushable;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.ExecutionConfig;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.io.RichOutputFormat;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.typeutils.InputTypeConfigurable;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.concurrent.ExecutorThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcOutputFormat<In, JdbcIn, JdbcExec extends JdbcBatchStatementExecutor<JdbcIn>>
      extends RichOutputFormat<In> implements Flushable, InputTypeConfigurable {

    protected final JdbcConnectionProvider connectionProvider;
    @Nullable
    private TypeSerializer<In> serializer;

    @Override
    @SuppressWarnings("unchecked")
    public void setInputType(TypeInformation<?> type, ExecutionConfig executionConfig) {
        if (executionConfig.isObjectReuseEnabled()) {
            this.serializer = (TypeSerializer<In>) type.createSerializer(executionConfig);
        }
    }

    public interface RecordExtractor<F, T> extends Function<F, T>, Serializable {
        static <T> RecordExtractor<T, T> identity() {
            return x -> x;
        }
    }

    public interface StatementExecutorFactory<T extends JdbcBatchStatementExecutor<?>>
          extends Function<RuntimeContext, T>, Serializable {
    }

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(JdbcOutputFormat.class);

    private final JdbcExecutionOptions executionOptions;
    private final StatementExecutorFactory<JdbcExec> statementExecutorFactory;
    private final RecordExtractor<In, JdbcIn> jdbcRecordExtractor;

    private transient JdbcExec jdbcStatementExecutor;
    private transient int batchCount = 0;
    private transient volatile boolean closed = false;

    private transient ScheduledExecutorService scheduler;
    private transient ScheduledFuture<?> scheduledFuture;
    private transient volatile Exception flushException;

    public JdbcOutputFormat(
          @Nonnull JdbcConnectionProvider connectionProvider,
          @Nonnull JdbcExecutionOptions executionOptions,
          @Nonnull StatementExecutorFactory<JdbcExec> statementExecutorFactory,
          @Nonnull RecordExtractor<In, JdbcIn> recordExtractor) {
        this.connectionProvider = checkNotNull(connectionProvider);
        this.executionOptions = checkNotNull(executionOptions);
        this.statementExecutorFactory = checkNotNull(statementExecutorFactory);
        this.jdbcRecordExtractor = checkNotNull(recordExtractor);
    }

    @Override
    public void configure(Configuration parameters) {
    }

    @Override
    public void open(int taskNumber, int numTasks) throws IOException {
        jdbcStatementExecutor = createAndOpenStatementExecutor(statementExecutorFactory);
        if (executionOptions.getBatchIntervalMs() != 0 && executionOptions.getBatchSize() != 1) {
            this.scheduler =
                  Executors.newScheduledThreadPool(
                        1, new ExecutorThreadFactory("jdbc-upsert-output-format"));
            this.scheduledFuture =
                  this.scheduler.scheduleWithFixedDelay(
                        () -> {
                            synchronized (JdbcOutputFormat.this) {
                                if (!closed) {
                                    try {
                                        flush();
                                    } catch (Exception e) {
                                        flushException = e;
                                    }
                                }
                            }
                        },
                        executionOptions.getBatchIntervalMs(),
                        executionOptions.getBatchIntervalMs(),
                        TimeUnit.MILLISECONDS);
        }
    }

    private JdbcExec createAndOpenStatementExecutor(
          StatementExecutorFactory<JdbcExec> statementExecutorFactory) throws IOException {
        JdbcExec exec = statementExecutorFactory.apply(getRuntimeContext());
        try {
            exec.prepareStatements(connectionProvider.getConnection());
        } catch (SQLException e) {
            throw new IOException("unable to open JDBC writer", e);
        }
        return exec;
    }

    private void checkFlushException() {
        if (flushException != null) {
            throw new RuntimeException("Writing records to JDBC failed.", flushException);
        }
    }

    @Override
    public final synchronized void writeRecord(In rec) throws IOException {
        checkFlushException();

        try {
            In recordCopy = copyIfNecessary(rec);
            JdbcIn extractedRecord = jdbcRecordExtractor.apply(recordCopy);
            jdbcStatementExecutor.addToBatch(extractedRecord);
            batchCount++;
            if (executionOptions.getBatchSize() > 0 && batchCount >= executionOptions.getBatchSize()) {
                flush();
            }
        } catch (Exception e) {
            throw new IOException("Writing records to JDBC failed.", e);
        }
    }

    private In copyIfNecessary(In rec) {
        return serializer == null ? rec : serializer.copy(rec);
    }


    @Override
    public synchronized void flush() throws IOException {
        checkFlushException();

        for (int i = 0; i <= executionOptions.getMaxRetries(); i++) {
            try {
                // make sure that a new connection was used for every flush to the database
                reconnect();
                attemptFlush();
                batchCount = 0;
                break;
            } catch (SQLException | ClassNotFoundException e) {
                LOG.error("JDBC executeBatch error, retry times = {}", i, e);
                if (i >= executionOptions.getMaxRetries()) {
                    throw new IOException(e);
                }
                try {
                    if (!connectionProvider.isConnectionValid()) {
                        reconnect();
                    }
                } catch (Exception exception) {
                    LOG.error(
                          "JDBC connection is not valid, and reestablish connection failed.",
                          exception);
                    throw new IOException("Reestablish JDBC connection failed", exception);
                }
                try {
                    Thread.sleep(1000 * i);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException(
                          "unable to flush; interrupted while doing another attempt", e);
                }
            }
        }
    }

    protected void attemptFlush() throws SQLException {
        jdbcStatementExecutor.executeBatch();
    }

    @Override
    public synchronized void close() {
        if (!closed) {
            closed = true;

            if (this.scheduledFuture != null) {
                scheduledFuture.cancel(false);
                this.scheduler.shutdown();
            }

            if (batchCount > 0) {
                try {
                    flush();
                } catch (Exception e) {
                    LOG.warn("Writing records to JDBC failed.", e);
                    throw new RuntimeException("Writing records to JDBC failed.", e);
                }
            }

            try {
                if (jdbcStatementExecutor != null) {
                    jdbcStatementExecutor.closeStatements();
                }
            } catch (SQLException e) {
                LOG.warn("Close JDBC writer failed.", e);
            }
        }
        connectionProvider.closeConnection();
        checkFlushException();
    }

    public void reconnect() throws SQLException, ClassNotFoundException {
        LOG.debug("Reestablishing connection to the database.");
        jdbcStatementExecutor.closeStatements();
        jdbcStatementExecutor.prepareStatements(getConnection());
    }

    @VisibleForTesting
    public Connection getConnection() {
        return connectionProvider.getConnection();
    }
}
