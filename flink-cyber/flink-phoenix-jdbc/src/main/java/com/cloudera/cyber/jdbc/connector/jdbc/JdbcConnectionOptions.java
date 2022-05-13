package com.cloudera.cyber.jdbc.connector.jdbc;

import org.apache.flink.util.Preconditions;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Optional;


public class JdbcConnectionOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String url;
    @Nullable protected final String driverName;
    protected final int connectionCheckTimeoutSeconds;
    @Nullable protected final String username;
    @Nullable protected final String password;

    protected JdbcConnectionOptions(
            String url,
            @Nullable String driverName,
            @Nullable String username,
            @Nullable String password,
            int connectionCheckTimeoutSeconds) {
        Preconditions.checkArgument(connectionCheckTimeoutSeconds > 0);
        this.url = Preconditions.checkNotNull(url, "jdbc url is empty");
        this.driverName = driverName;
        this.username = username;
        this.password = password;
        this.connectionCheckTimeoutSeconds = connectionCheckTimeoutSeconds;
    }

    public String getDbURL() {
        return url;
    }

    @Nullable
    public String getDriverName() {
        return driverName;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public int getConnectionCheckTimeoutSeconds() {
        return connectionCheckTimeoutSeconds;
    }

    public static class JdbcConnectionOptionsBuilder {
        private String url;
        private String driverName;
        private String username;
        private String password;
        private int connectionCheckTimeoutSeconds = 60;

        public JdbcConnectionOptionsBuilder withUrl(String url) {
            this.url = url;
            return this;
        }

        public JdbcConnectionOptionsBuilder withDriverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        public JdbcConnectionOptionsBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public JdbcConnectionOptionsBuilder withPassword(String password) {
            this.password = password;
            return this;
        }

        public JdbcConnectionOptionsBuilder withConnectionCheckTimeoutSeconds(
                int connectionCheckTimeoutSeconds) {
            this.connectionCheckTimeoutSeconds = connectionCheckTimeoutSeconds;
            return this;
        }

        public JdbcConnectionOptions build() {
            return new JdbcConnectionOptions(
                    url, driverName, username, password, connectionCheckTimeoutSeconds);
        }
    }
}
