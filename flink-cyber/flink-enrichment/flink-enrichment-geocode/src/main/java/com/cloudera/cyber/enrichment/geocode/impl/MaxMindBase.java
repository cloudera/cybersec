package com.cloudera.cyber.enrichment.geocode.impl;

import com.cloudera.cyber.cache.Memoizer;
import com.google.common.base.Preconditions;
import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.Enrichment;
import com.maxmind.db.CHMCache;
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
import java.util.function.Function;

@Slf4j
public abstract class MaxMindBase {
    private static final Function<String, DatabaseProvider> MEMOIZER = Memoizer.memoize(MaxMindBase::loadDatabaseProvider);
    public static final String FIELD_VALUE_IS_NOT_A_STRING = "'%s' is not a String.";
    public static final String FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS = "'%s' is not a valid IP address.";
    public static final String MAXMIND_FAILED_MESSAGE = "Maxmind lookup failed '%s'";

    /**
     * Parsed and cached Maxmind database.
     */
    @NonNull
    protected final DatabaseProvider database;

    protected MaxMindBase(DatabaseProvider database) {
        Preconditions.checkNotNull(database);
        this.database = database;
    }

    protected MaxMindBase(String geocodeDatabasePath) {
        Preconditions.checkArgument(StringUtils.isNotBlank(geocodeDatabasePath), "The path to Maxmind database is blank '%s'", geocodeDatabasePath);
        DatabaseProvider databaseProvider = getDatabaseProvider(geocodeDatabasePath);
        Preconditions.checkNotNull(databaseProvider);
        this.database = databaseProvider;
    }

    protected static DatabaseProvider getDatabaseProvider(String geocodeDatabasePath) {
        return MEMOIZER.apply(geocodeDatabasePath);
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
