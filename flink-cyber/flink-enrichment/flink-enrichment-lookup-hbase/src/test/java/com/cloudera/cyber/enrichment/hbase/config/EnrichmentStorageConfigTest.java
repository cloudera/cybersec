package com.cloudera.cyber.enrichment.hbase.config;

import org.junit.Test;

import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat.HBASE_METRON;
import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat.HBASE_SIMPLE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnrichmentStorageConfigTest {

    private static final String TEST_ENRICHMENT_TYPE = "TestEnrichmentType";
    @Test
    public void testValidSimpleHbase() {
        testStorageConfig(HBASE_SIMPLE, "table", "");
        testStorageConfig(HBASE_SIMPLE, "table", null);
    }

    @Test
    public void testValidMetronHbase() {
        testStorageConfig(HBASE_METRON, "table", "cf");
    }

    @Test
    public void testMissingTableName() {
        testInvalidStorageConfig(EnrichmentStorageConfig.STORAGE_CONFIG_TABLE_NOT_SET_ERROR, HBASE_METRON, null, "cf");
        testInvalidStorageConfig(EnrichmentStorageConfig.STORAGE_CONFIG_TABLE_NOT_SET_ERROR, HBASE_METRON, "", "cf");

        testInvalidStorageConfig(EnrichmentStorageConfig.STORAGE_CONFIG_TABLE_NOT_SET_ERROR, HBASE_SIMPLE, null, "");
        testInvalidStorageConfig(EnrichmentStorageConfig.STORAGE_CONFIG_TABLE_NOT_SET_ERROR, HBASE_SIMPLE, "", "");
    }

    @Test
    public void testMissingColumnFamily() {
        testInvalidStorageConfig(EnrichmentStorageConfig.STORAGE_CONFIG_COLUMN_FAMILY_NOT_SET_ERROR, HBASE_METRON, "table", null);
        testInvalidStorageConfig(EnrichmentStorageConfig.STORAGE_CONFIG_COLUMN_FAMILY_NOT_SET_ERROR, HBASE_METRON, "table", "");
    }

    @Test
    public void testInvalidColumnFamily() {
        testInvalidStorageConfig(EnrichmentStorageConfig.STORAGE_CONFIG_COLUMN_FAMILY_SET_ERROR, HBASE_SIMPLE, "table", "cf");
    }

    private void testStorageConfig(EnrichmentStorageFormat format, String hbaseTableName, String columnFamily) {
        EnrichmentStorageConfig storageConfig = new EnrichmentStorageConfig(format, hbaseTableName, columnFamily);
        storageConfig.validate(TEST_ENRICHMENT_TYPE);
    }

    private void testInvalidStorageConfig(String expectedMessage, EnrichmentStorageFormat format, String hbaseTableName, String columnFamily) {
        assertThatThrownBy(() -> testStorageConfig(format, hbaseTableName, columnFamily)).isInstanceOf(IllegalStateException.class)
                .hasMessage(expectedMessage, TEST_ENRICHMENT_TYPE);
    }
}
