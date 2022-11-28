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

import org.apache.flink.util.Preconditions;

import java.io.Serializable;
import java.util.Objects;

public class JdbcExecutionOptions implements Serializable {
    public static final int DEFAULT_MAX_RETRY_TIMES = 3;
    private static final int DEFAULT_INTERVAL_MILLIS = 0;
    public static final int DEFAULT_SIZE = 5000;

    private final long batchIntervalMs;
    private final int batchSize;
    private final int maxRetries;

    private JdbcExecutionOptions(long batchIntervalMs, int batchSize, int maxRetries) {
        Preconditions.checkArgument(maxRetries >= 0);
        this.batchIntervalMs = batchIntervalMs;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
    }

    public long getBatchIntervalMs() {
        return batchIntervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JdbcExecutionOptions that = (JdbcExecutionOptions) o;
        return batchIntervalMs == that.batchIntervalMs
                && batchSize == that.batchSize
                && maxRetries == that.maxRetries;
    }

    @Override
    public int hashCode() {
        return Objects.hash(batchIntervalMs, batchSize, maxRetries);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static JdbcExecutionOptions defaults() {
        return builder().build();
    }

    public static final class Builder {
        private long intervalMs = DEFAULT_INTERVAL_MILLIS;
        private int size = DEFAULT_SIZE;
        private int maxRetries = DEFAULT_MAX_RETRY_TIMES;

        public Builder withBatchSize(int size) {
            this.size = size;
            return this;
        }

        public Builder withBatchIntervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
            return this;
        }

        public Builder withMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public JdbcExecutionOptions build() {
            return new JdbcExecutionOptions(intervalMs, size, maxRetries);
        }
    }
}
