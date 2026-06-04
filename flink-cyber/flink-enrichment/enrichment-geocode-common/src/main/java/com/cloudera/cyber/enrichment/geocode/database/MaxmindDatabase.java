package com.cloudera.cyber.enrichment.geocode.database;

import com.maxmind.db.CHMCache;
import com.maxmind.db.Reader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import org.apache.flink.util.Preconditions;

@Slf4j
public class MaxmindDatabase implements AutoCloseable {
    private static final ConcurrentHashMap<String, Reader> READER_CACHE = new ConcurrentHashMap<>();
    private static final String EXTENSION_MMDB = ".mmdb";
    private static final String EXTENSION_TAR_GZ = ".tar.gz";
    private static final String EXTENSION_MMDB_GZ = ".mmdb.gz";

    protected Reader database;
    private final String databasePath;
    protected final MaxmindDatabaseVendor databaseVendor;

    public MaxmindDatabase(Reader database, String databasePath) {
        Preconditions.checkNotNull(database);
        this.database = database;
        this.databasePath = databasePath;
        this.databaseVendor = getVendor(database);
    }

    public MaxmindDatabase(String geocodeDatabasePath) {
        this(getDatabaseProvider(geocodeDatabasePath), geocodeDatabasePath);
    }

    public void close() throws IOException {
        this.database = READER_CACHE.compute(databasePath, MaxmindDatabase::cleanupReader);
    }

    private static Reader cleanupReader(String databasePath, Reader reader) {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ioe) {
                log.error("Closing reader failed.");
            }
        }
        return null;
    }

    private static com.maxmind.db.Reader getDatabaseProvider(String geocodeDatabasePath) {
        Preconditions.checkArgument(geocodeDatabasePath != null && !geocodeDatabasePath.isBlank(), "The path to Maxmind database is blank '%s'", geocodeDatabasePath);
        return READER_CACHE.compute(geocodeDatabasePath, MaxmindDatabase::intializeReader);
    }

    private static Reader intializeReader(String databasePath, Reader reader) {
        if (reader != null) {
            return reader;
        } else {
            return loadDatabaseProvider(databasePath);
        }
    }

    private static com.maxmind.db.Reader loadDatabaseProvider(String geocodeDatabasePath) {
        log.info("Loading ip enrichment database {}", geocodeDatabasePath);
        Reader reader;
        try {
            FileSystem fileSystem = new Path(geocodeDatabasePath).getFileSystem();
            reader = createDatabaseReader(geocodeDatabasePath, fileSystem);
        } catch (IOException ioe) {
            log.error("Unable to load file maxmind database {}", geocodeDatabasePath, ioe);
            throw new IllegalStateException(String.format("Could not read geocode database '%s'.", geocodeDatabasePath), ioe);
        }
        return reader;
    }

    private static Reader createDatabaseReader(String geocodeDatabasePath, FileSystem fileSystem) throws IOException {
        Reader reader;
        try (InputStream dbStream = createDatabaseInputStream(geocodeDatabasePath, fileSystem)) {
            reader = new Reader(dbStream, new CHMCache());
            log.info("Successfully loaded {} database {}", reader.getMetadata().databaseType(), geocodeDatabasePath);
        }
        return reader;
    }

    private static InputStream createDatabaseInputStream(String geocodeDatabasePath, FileSystem fileSystem) throws IOException {
        if (geocodeDatabasePath.endsWith(EXTENSION_MMDB)) {
            return new BufferedInputStream(fileSystem.open(new Path(geocodeDatabasePath)));
        } else if (geocodeDatabasePath.endsWith(EXTENSION_MMDB_GZ)) {
            return new GZIPInputStream(new BufferedInputStream(fileSystem.open(new Path(geocodeDatabasePath))));
        } else if (geocodeDatabasePath.endsWith(EXTENSION_TAR_GZ)) {
            TarArchiveInputStream is = new TarArchiveInputStream(
                new GzipCompressorInputStream(new BufferedInputStream(fileSystem.open(new Path(geocodeDatabasePath)))));
            // Need to find the mmdb entry.
            TarArchiveEntry entry = is.getNextEntry();
            while (entry != null) {
                if (entry.isFile() && entry.getName().endsWith(EXTENSION_MMDB)) {
                    return is;
                }
                entry = is.getNextEntry();
            }
            is.close();
            log.error("Maxmind tar archive file '{}' does not contain an mmdb file.", geocodeDatabasePath);
            throw new IllegalStateException(String.format("Maxmind tar archive file '%s' does not contain an mmdb file.", geocodeDatabasePath));
        }
        log.error("Maxmind file '{}' has an unsupported extension.", geocodeDatabasePath);
        throw new IllegalStateException(String.format("Maxmind database file '%s' has an unsupported extension.  Supported extensions are: %s", geocodeDatabasePath, String.join(", ", EXTENSION_MMDB, EXTENSION_MMDB_GZ, EXTENSION_TAR_GZ)));
    }

    private static MaxmindDatabaseVendor getVendor(Reader reader) {
        String databaseType = reader.getMetadata().databaseType().toLowerCase();

        if (databaseType.contains("geoip") || databaseType.contains("geolite")) {
            return MaxmindDatabaseVendor.MAXMIND;
        } else if (databaseType.contains("ipinfo")) {
            return MaxmindDatabaseVendor.IPINFO;
        } else {
            throw new IllegalStateException(String.format("MMDB file with type '%s' is not supported.", databaseType));
        }
    }

    protected MaxmindDatabaseVendor getDatabaseVendor() {
        return databaseVendor;
    }
}
