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

package com.cloudera.cyber.enrichment.geocode.impl;

import com.cloudera.cyber.cache.Memoizer;
import com.google.common.base.Preconditions;
import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.Enrichment;
import com.maxmind.db.CHMCache;
import com.maxmind.db.Reader;
import com.maxmind.geoip2.DatabaseProvider;
import com.maxmind.geoip2.DatabaseReader;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
public abstract class MaxMindBase {
    private static final Function<String, DatabaseProvider> MEMOIZER = Memoizer.memoize(MaxMindBase::loadDatabaseProvider);
    public static final String FIELD_VALUE_IS_NOT_A_STRING = "'%s' is not a String.";
    public static final String FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS = "'%s' is not a valid IP address.";
    public static final String MAXMIND_FAILED_MESSAGE = "Maxmind lookup failed '%s'";

    /**
     * Parsed and cached Maxmind database ( GeoIP2 typed responses).
     */
    @NonNull
    protected final DatabaseProvider database;

    /**
     * Generic MaxMind DB reader that can read both MaxMind and IPinfo MMDB files.
     * This provides support for IPinfo databases that use a different schema than MaxMind.
     */
    @NonNull
    protected final Reader maxmindDbReader;

    protected MaxMindBase(DatabaseProvider database) {
        Preconditions.checkNotNull(database);
        this.database = database;
        this.maxmindDbReader = null;
    }

    protected MaxMindBase(DatabaseProvider database, Reader maxmindDbReader) {
        Preconditions.checkNotNull(database);
        Preconditions.checkNotNull(maxmindDbReader);
        this.database = database;
        this.maxmindDbReader = maxmindDbReader;
    }

    /**
     * Constructor that accepts only a maxmind-db Reader (for IPinfo or other MMDB databases).
     * Note: This constructor sets database to null since we're using the generic Reader.
     *
     * @param maxmindDbReader The generic maxmind-db Reader
     */
    protected MaxMindBase(Reader maxmindDbReader) {
        Preconditions.checkNotNull(maxmindDbReader);
        this.database = null;
        this.maxmindDbReader = maxmindDbReader;
    }

    protected MaxMindBase(String geocodeDatabasePath) {
        Preconditions.checkArgument(StringUtils.isNotBlank(geocodeDatabasePath), "The path to Maxmind database is blank '%s'", geocodeDatabasePath);
        DatabaseProvider databaseProvider = getDatabaseProvider(geocodeDatabasePath);
        Reader genericReader = getMaxmindDbReader(geocodeDatabasePath);
        Preconditions.checkNotNull(databaseProvider);
        Preconditions.checkNotNull(genericReader);
        this.database = databaseProvider;
        this.maxmindDbReader = genericReader;
    }

    protected static DatabaseProvider getDatabaseProvider(String geocodeDatabasePath) {
        return MEMOIZER.apply(geocodeDatabasePath);
    }

    private static final Function<String, Reader> MEMOIZER_DB = Memoizer.memoize(MaxMindBase::loadMaxmindDbReader);

    protected static Reader getMaxmindDbReader(String geocodeDatabasePath) {
        return MEMOIZER_DB.apply(geocodeDatabasePath);
    }

    private static Reader loadMaxmindDbReader(String geocodeDatabasePath) {
        log.info("Loading Maxmind DB reader {}", geocodeDatabasePath);
        Reader reader = null;
        try {
            FileSystem fileSystem = new Path(geocodeDatabasePath).getFileSystem();
            reader = createMaxmindDbReader(geocodeDatabasePath, fileSystem);
        } catch (IOException ioe) {
            log.error("Unable to load file system {}", geocodeDatabasePath, ioe);
            throw new IllegalStateException(String.format("Could not read geocode database %s", geocodeDatabasePath));
        }
        return reader;
    }

    private static Reader createMaxmindDbReader(String geocodeDatabasePath, FileSystem fileSystem) throws IOException {
        Reader reader;
        try (FSDataInputStream dbStream = fileSystem.open(new Path(geocodeDatabasePath))) {
            reader = new Reader.Builder(dbStream).withCache(new CHMCache()).build();
            log.info("Successfully loaded Maxmind DB reader {}", geocodeDatabasePath);
        } catch (Exception e) {
            log.error("Exception while loading geocode database {}", geocodeDatabasePath, e);
            throw e;
        }
        return reader;
    }

    private static DatabaseProvider loadDatabaseProvider(String geocodeDatabasePath) {
        log.info("Loading Maxmind database {}", geocodeDatabasePath);
        DatabaseReader reader = null;
        try {
            FileSystem fileSystem = new Path(geocodeDatabasePath).getFileSystem();
            reader = createDatabaseReader(geocodeDatabasePath, fileSystem);
        } catch (IOException ioe) {
            log.error("Unable to load file system {}", geocodeDatabasePath, ioe);
            throw new IllegalStateException(String.format("Could not read geocode database %s", geocodeDatabasePath));
        }
        return reader;
    }

    private static DatabaseReader createDatabaseReader(String geocodeDatabasePath, FileSystem fileSystem) throws IOException {
        DatabaseReader reader;
        try (FSDataInputStream dbStream = fileSystem.open(new Path(geocodeDatabasePath))) {
            reader = new DatabaseReader.Builder(dbStream).withCache(new CHMCache()).build();
            log.info("Successfully loaded Maxmind database {}", geocodeDatabasePath);
        } catch (Exception e) {
            log.error("Exception while loading geocode database {}", geocodeDatabasePath, e);
            throw e;
        }
        return reader;
    }

    /**
     * Perform a generic lookup using the maxmind-db Reader.
     * This method works with both MaxMind and IPinfo MMDB files.
     *
     * @param ipAddress The IP address to look up
     * @return The result as a Map, or null if the lookup failed
     */
    @SuppressWarnings("unchecked")
    protected Map<String, Object> lookupGeneric(InetAddress ipAddress) {
        if (maxmindDbReader == null) {
            log.warn("maxmindDbReader is not initialized");
            return null;
        }
        try {
            return maxmindDbReader.get(ipAddress);
        } catch (Exception e) {
            log.debug("Generic lookup failed for IP: {}", ipAddress, e);
            return null;
        }
    }

    /**
     * Perform a generic lookup using the maxmind-db Reader with IP address string.
     * This method works with both MaxMind and IPinfo MMDB files.
     *
     * @param ipAddressString The IP address string to look up
     * @return The result as a Map, or null if the lookup failed
     */
    protected Map<String, Object> lookupGeneric(String ipAddressString) {
        try {
            InetAddress ipAddress = InetAddress.getByName(ipAddressString);
            return lookupGeneric(ipAddress);
        } catch (UnknownHostException e) {
            log.debug("Invalid IP address: {}", ipAddressString);
            return null;
        }
    }


    protected InetAddress convertToIpAddress(Enrichment enrichment, Object ipValueObject, List<DataQualityMessage> qualityMessages) {
        InetAddress inetAddress = null;
        if (ipValueObject instanceof String) {
            String ipValue = (String) ipValueObject;
            if (InetAddressValidator.getInstance().isValid(ipValue)) {
                try {
                    inetAddress = InetAddress.getByName(ipValue);
                    if (inetAddress.isSiteLocalAddress() ||
                            inetAddress.isAnyLocalAddress() ||
                            inetAddress.isLinkLocalAddress() ||
                            inetAddress.isLoopbackAddress() ||
                            inetAddress.isMulticastAddress()) {
                        // internal network addresses won't have geo info so stop here
                        inetAddress = null;
                    }
                } catch (UnknownHostException e) {
                    // this should not happen - checks for valid IP prior to call
                    enrichment.addQualityMessage(qualityMessages, DataQualityMessageLevel.INFO, String.format(MAXMIND_FAILED_MESSAGE, e.getMessage()));
                }
            } else {
                enrichment.addQualityMessage(qualityMessages, DataQualityMessageLevel.INFO, String.format(FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS, ipValue));
            }
        } else {
            enrichment.addQualityMessage(qualityMessages, DataQualityMessageLevel.INFO, String.format(FIELD_VALUE_IS_NOT_A_STRING, ipValueObject.toString()));
        }

        return inetAddress;
    }

}
