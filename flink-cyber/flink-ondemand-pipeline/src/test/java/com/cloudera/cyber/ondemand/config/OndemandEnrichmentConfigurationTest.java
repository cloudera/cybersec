package com.cloudera.cyber.ondemand.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;

import static com.cloudera.cyber.ondemand.config.OnDemandEnrichmentConfigurationHelper.DEFAULT_START_TIME;
import static com.cloudera.cyber.ondemand.config.OnDemandEnrichmentConfigurationHelper.DEFAULT_END_TIME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OndemandEnrichmentConfigurationTest {
    private static final String DESTINATION_DATABASE = "ondemand";

    private static final OnDemandEnrichmentConfigurationHelper helper = new OnDemandEnrichmentConfigurationHelper();

    @TempDir
    Path tempDir;

    @Test
    void testReadConfiguration() throws Exception {
        OndemandEnrichmentConfiguration configuration = helper.readConfiguration(tempDir, "squid", DESTINATION_DATABASE);

        assertEquals(LocalDateTime.of(2026, Month.SEPTEMBER, 1, 17, 15, 0, 35000000), configuration.startTime());
        assertEquals(LocalDateTime.of(2026, Month.SEPTEMBER, 2, 18, 30, 0), configuration.endTime());
        assertEquals(List.of("squid"), configuration.sourcesToEnrich());
        assertEquals(DESTINATION_DATABASE, configuration.destinationDatabase());
        assertTrue(configuration.indexTableMappingConfiguration().getUnenrichedTopicMapping().containsKey("squid"));

        // record generated members
        assertEquals(configuration, helper.readConfiguration(tempDir, "squid", DESTINATION_DATABASE));
        assertNotEquals(configuration, helper.readConfiguration(tempDir, "squid", "other_database"));
        assertTrue(configuration.toString().contains(DESTINATION_DATABASE));
    }

    @Test
    void testReadConfigurationDeduplicatesSources() throws Exception {
        OndemandEnrichmentConfiguration configuration = helper.readConfiguration(tempDir, "squid,squid", DESTINATION_DATABASE);

        assertEquals(List.of("squid"), configuration.sourcesToEnrich());
    }

    @Test
    void testReadConfigurationRejectsStartTimeAfterEndTime() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> helper.readConfiguration(tempDir, "squid", DESTINATION_DATABASE,
                        DEFAULT_END_TIME, DEFAULT_START_TIME));

        assertTrue(exception.getMessage().contains("must be earlier than the end time"));
    }

    @Test
    void testReadConfigurationRejectsNonIcebergConnector() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> helper.readConfiguration(tempDir, "squid", DESTINATION_DATABASE, "hive"));

        assertTrue(exception.getMessage().contains("Only iceberg is supported"));
    }

    @Test
    void testReadConfigurationRejectsSameDestinationDatabase() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> helper.readConfiguration(tempDir, "squid", OnDemandEnrichmentConfigurationHelper.INDEX_DATABASE));

        assertTrue(exception.getMessage().contains("must be different"));
    }

    @Test
    void testReadConfigurationRejectsUnmappedSources() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> helper.readConfiguration(tempDir, "squid,unknown_source", DESTINATION_DATABASE));

        assertTrue(exception.getMessage().contains("unknown_source"));
    }

    @Test
    void testNoErrorsReportPath() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> helper.readConfiguration(tempDir, "squid", "dest_db",
                        "iceberg", DEFAULT_START_TIME, DEFAULT_END_TIME, null));
        assertTrue(exception.getMessage().contains("No data for required key 'report.errors.path'"));
    }

}
