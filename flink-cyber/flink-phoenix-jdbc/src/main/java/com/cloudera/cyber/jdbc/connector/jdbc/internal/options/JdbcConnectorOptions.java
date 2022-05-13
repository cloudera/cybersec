/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.cyber.jdbc.connector.jdbc.internal.options;

import com.cloudera.cyber.jdbc.connector.jdbc.JdbcConnectionOptions;

import javax.annotation.Nullable;
import java.util.Objects;

import static org.apache.flink.util.Preconditions.checkNotNull;


public class JdbcConnectorOptions extends JdbcConnectionOptions {

    private static final long serialVersionUID = 1L;

    private final String tableName;
    private final @Nullable Integer parallelism;

    private JdbcConnectorOptions(
            String dbURL,
            String tableName,
            @Nullable String driverName,
            @Nullable String username,
            @Nullable String password,
            @Nullable Integer parallelism,
            int connectionCheckTimeoutSeconds) {
        super(dbURL, driverName, username, password, connectionCheckTimeoutSeconds);
        this.tableName = tableName;
        this.parallelism = parallelism;
    }

    public String getTableName() {
        return tableName;
    }


    public Integer getParallelism() {
        return parallelism;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JdbcConnectorOptions) {
            JdbcConnectorOptions options = (JdbcConnectorOptions) o;
            return Objects.equals(url, options.url)
                    && Objects.equals(tableName, options.tableName)
                    && Objects.equals(driverName, options.driverName)
                    && Objects.equals(username, options.username)
                    && Objects.equals(password, options.password)
                    && Objects.equals(parallelism, options.parallelism)
                    && Objects.equals(
                    connectionCheckTimeoutSeconds, options.connectionCheckTimeoutSeconds);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                url,
                tableName,
                driverName,
                username,
                password,
                parallelism,
                connectionCheckTimeoutSeconds);
    }

    public static class Builder {
        private String dbURL;
        private String tableName;
        private String driverName;
        private String username;
        private String password;
        private Integer parallelism;
        private int connectionCheckTimeoutSeconds = 60;

        public Builder setTableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public Builder setConnectionCheckTimeoutSeconds(int connectionCheckTimeoutSeconds) {
            this.connectionCheckTimeoutSeconds = connectionCheckTimeoutSeconds;
            return this;
        }


        public Builder setDriverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        /**
         * required, JDBC DB url.
         */
        public Builder setDBUrl(String dbURL) {
            this.dbURL = dbURL;
            return this;
        }


        public Builder setParallelism(Integer parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        public JdbcConnectorOptions build() {
            checkNotNull(dbURL, "No dbURL supplied.");
            checkNotNull(tableName, "No tableName supplied.");

            if (this.driverName == null) {
                throw new NullPointerException("No driverName supplied.");
            }

            return new JdbcConnectorOptions(
                    dbURL,
                    tableName,
                    driverName,
                    username,
                    password,
                    parallelism,
                    connectionCheckTimeoutSeconds);
        }
    }
}
