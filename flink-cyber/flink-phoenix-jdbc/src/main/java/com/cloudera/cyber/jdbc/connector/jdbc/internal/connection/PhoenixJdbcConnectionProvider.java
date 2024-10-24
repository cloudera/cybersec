/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.cyber.jdbc.connector.jdbc.internal.connection;

import com.cloudera.cyber.jdbc.connector.jdbc.JdbcConnectionOptions;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import javax.annotation.concurrent.NotThreadSafe;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NotThreadSafe
public class PhoenixJdbcConnectionProvider implements JdbcConnectionProvider, Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(PhoenixJdbcConnectionProvider.class);

    private static final long serialVersionUID = 1L;

    private final JdbcConnectionOptions jdbcOptions;

    private transient Driver loadedDriver;

    static {
        // Load DriverManager first to avoid deadlock between DriverManager's
        // static initialization block and speciAbstractJdbcOutputFormatfic driver class's static
        // initialization block when two different driver classes are loading
        // concurrently using Class.forName while DriverManager is uninitialized
        // before.
        //
        // This could happen in JDK 8 but not above as driver loading has been
        // moved out of DriverManager's static initialization block since JDK 9.
        DriverManager.getDrivers();
    }

    public PhoenixJdbcConnectionProvider(JdbcConnectionOptions jdbcOptions) {
        this.jdbcOptions = jdbcOptions;
    }

    @Override
    public Connection getConnection() {
        try {
            return getOrEstablishConnection();
        } catch (SQLException | ClassNotFoundException throwables) {
            LOG.error(throwables.getMessage());
        }
        return null;
    }

    @Override
    public boolean isConnectionValid() {
        return false;
    }

    private static Driver loadDriver(String driverName)
          throws SQLException, ClassNotFoundException {
        Preconditions.checkNotNull(driverName);
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getName().equals(driverName)) {
                return driver;
            }
        }
        // We could reach here for reasons:
        // * Class loader hell of DriverManager(see JDK-8146872).
        // * driver is not installed as a service provider.
        Class<?> clazz =
              Class.forName(driverName, true, Thread.currentThread().getContextClassLoader());
        try {
            return (Driver) clazz.newInstance();
        } catch (Exception ex) {
            throw new SQLException("Fail to create driver of class " + driverName, ex);
        }
    }

    private Driver getLoadedDriver() throws SQLException, ClassNotFoundException {
        if (loadedDriver == null) {
            loadedDriver = loadDriver(jdbcOptions.getDriverName());
        }
        return loadedDriver;
    }

    @Override
    public Connection getOrEstablishConnection() throws SQLException, ClassNotFoundException {
        if (jdbcOptions.getDriverName() == null) {
            LOG.debug("Establishing anonymous connection to the {}.", jdbcOptions.getDbURL());
            return DriverManager.getConnection(jdbcOptions.getDbURL());
        } else {
            LOG.debug("Establishing connection to the {}.", jdbcOptions.getDbURL());
            Driver driver = getLoadedDriver();
            Properties info = new Properties();
            jdbcOptions.getUsername().ifPresent(user -> info.setProperty("user", user));
            jdbcOptions.getPassword().ifPresent(password -> info.setProperty("password", password));
            return driver.connect(jdbcOptions.getDbURL(), info);
        }
    }

    @Override
    public void closeConnection() {
    }

    @Override
    public Connection reestablishConnection() throws SQLException, ClassNotFoundException {
        return getOrEstablishConnection();
    }
}
