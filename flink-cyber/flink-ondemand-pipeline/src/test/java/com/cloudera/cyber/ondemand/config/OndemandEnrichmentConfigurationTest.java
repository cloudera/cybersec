package com.cloudera.cyber.ondemand.config;

import com.cloudera.cyber.indexing.hive.tableapi.TableApiJobFactory;
import org.apache.flink.api.java.utils.ParameterTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OndemandEnrichmentConfigurationTest {

    private static final String DESTINATION_DATABASE = "ondemand";
    private static final String INDEX_DATABASE = "cyber";

    @TempDir
    Path tempDir;

    @Test
    void testReadConfiguration() throws Exception {
        OndemandEnrichmentConfiguration configuration = readConfiguration("squid", DESTINATION_DATABASE);

        assertEquals(LocalDateTime.of(2026, Month.SEPTEMBER, 1, 17, 15, 0, 35000000), configuration.startTime());
        assertEquals(LocalDateTime.of(2026, Month.SEPTEMBER, 2, 18, 30, 0), configuration.endTime());
        assertEquals(List.of("squid"), configuration.sourcesToEnrich());
        assertEquals(DESTINATION_DATABASE, configuration.destinationDatabase());
        assertTrue(configuration.indexTableMappingConfiguration().getUnenrichedTopicMapping().containsKey("squid"));

        // record generated members
        assertEquals(configuration, readConfiguration("squid", DESTINATION_DATABASE));
        assertNotEquals(configuration, readConfiguration("squid", "other_database"));
        assertTrue(configuration.toString().contains(DESTINATION_DATABASE));
    }

    @Test
    void testReadConfigurationDeduplicatesSources() throws Exception {
        OndemandEnrichmentConfiguration configuration = readConfiguration("squid,squid", DESTINATION_DATABASE);

        assertEquals(List.of("squid"), configuration.sourcesToEnrich());
    }

    @Test
    void testReadConfigurationRejectsNonIcebergConnector() throws Exception {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readConfiguration("squid", DESTINATION_DATABASE, "hive"));

        assertTrue(exception.getMessage().contains("Only iceberg is supported"));
    }

    @Test
    void testReadConfigurationRejectsSameDestinationDatabase() throws Exception {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readConfiguration("squid", INDEX_DATABASE));

        assertTrue(exception.getMessage().contains("must be different"));
    }

    @Test
    void testReadConfigurationRejectsUnmappedSources() throws Exception {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> readConfiguration("squid,unknown_source", DESTINATION_DATABASE));

        assertTrue(exception.getMessage().contains("unknown_source"));
    }

    private OndemandEnrichmentConfiguration readConfiguration(String sources, String destinationDatabase)
            throws Exception {
        return readConfiguration(sources, destinationDatabase, TableApiJobFactory.ICEBERG_INDEX_JOB_CONNECTOR);
    }

    private OndemandEnrichmentConfiguration readConfiguration(String sources, String destinationDatabase, String connector)
            throws Exception {
        String suffix = connector + "-" + destinationDatabase;

        Path triageFile = tempDir.resolve("triage-" + suffix + ".properties");
        Files.writeString(triageFile, String.join("\n",
                "geo.enabled=false",
                "asn.enabled=false",
                "company.enabled=false",
                "lookups.config.file=" + resourcePath("ondemand-lookups.json")));

        Path indexFile = tempDir.resolve("index-" + suffix + ".properties");
        Files.writeString(indexFile, String.join("\n",
                "flink.output-connector=" + connector,
                "hive.dbname=" + INDEX_DATABASE,
                "flink.mapping-file=" + resourcePath("ondemand-index-mapping.json"),
                "flink.tables-init-file=" + resourcePath("ondemand-index-tables.json")));

        Map<String, String> params = new HashMap<>();
        params.put("sources", sources);
        params.put("triage.config", triageFile.toString());
        params.put("index.config", indexFile.toString());
        params.put("start.timestamp", "2026-09-01 17:15:00.035");
        params.put("end.timestamp", "2026-09-02 18:30:00.000");
        params.put("destination.hive.dbname", destinationDatabase);

        return OndemandEnrichmentConfiguration.readConfiguration(ParameterTool.fromMap(params));
    }

    private String resourcePath(String name) throws Exception {
        return new File(getClass().getClassLoader().getResource(name).toURI()).getAbsolutePath();
    }
}
