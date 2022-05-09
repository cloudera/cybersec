package com.cloudera.cyber.enrichment.hbase.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentFieldsConfigTest.KEY_FIELDS;
import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat.HBASE_METRON;
import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat.HBASE_SIMPLE;
import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig.DEFAULT_ENRICHMENT_STORAGE_NAME;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


public class EnrichmentsConfigTest {

    public static final EnrichmentStorageConfig DEFAULT_STORAGE = new EnrichmentStorageConfig(HBASE_METRON, "enrichments", "cf");
    public static final Map<String, EnrichmentStorageConfig> DEFAULT_STORAGE_CONFIGS = ImmutableMap.of(DEFAULT_ENRICHMENT_STORAGE_NAME, DEFAULT_STORAGE);

    @Test
    public void testEmptyConfigs() {
        new EnrichmentsConfig(DEFAULT_STORAGE_CONFIGS, null).validate();
    }

    @Test
    public void testMissingDefaultStorage() {
        EnrichmentsConfig config = new EnrichmentsConfig(null, null);
        assertThatThrownBy(config::validate).isInstanceOf(IllegalStateException.class)
                .hasMessage(EnrichmentsConfig.MISSING_STORAGE_ERROR, DEFAULT_ENRICHMENT_STORAGE_NAME);
    }

    @Test
    public void testValidConfigs() {
        createValidEnrichmentsConfig().validate();
    }

    @Test
    public void testInvalidConfigs() {
        EnrichmentsConfig invalidConfig = createValidEnrichmentsConfig();
        String invalidEnrichName = "invalid_enrich";
        invalidConfig.getEnrichmentConfigs().put(invalidEnrichName, new EnrichmentConfig(null,
                new EnrichmentFieldsConfig(null, null, null, null)));
        assertThatThrownBy(invalidConfig::validate).isInstanceOf(IllegalStateException.class)
                .hasMessage(EnrichmentFieldsConfig.FIELD_CONFIG_INVALID_KEY_FIELD, invalidEnrichName);
    }

    @Test
    public void testNullEnrichmentName() {
        EnrichmentsConfig config = new EnrichmentsConfig(DEFAULT_STORAGE_CONFIGS, null);
        config.getEnrichmentConfigs().put("", new EnrichmentConfig());

        assertThatThrownBy(config::validate).isInstanceOf(IllegalStateException.class)
                .hasMessage(EnrichmentsConfig.NO_ENRICHMENT_TYPE_NAME_SPECIFIED_ERROR);
    }

    @Test
    public void testNullStorageName() {
        Map<String, EnrichmentStorageConfig> badStorageConfig = new HashMap<>(DEFAULT_STORAGE_CONFIGS);
        badStorageConfig.put("", new EnrichmentStorageConfig());
        EnrichmentsConfig config = new EnrichmentsConfig(badStorageConfig, null);

        assertThatThrownBy(config::validate).isInstanceOf(IllegalStateException.class)
                .hasMessage(EnrichmentsConfig.NO_STORAGE_TYPE_NAME_SPECIFIED_ERROR);
    }
    @Test
    public void testLoad() {
        EnrichmentsConfig config = testLoadJson(getJsonAbsPath("enrichments_config.json"));
        Assert.assertEquals(2, config.getEnrichmentConfigs().size());
        Assert.assertEquals(HBASE_METRON, config.getStorageForEnrichmentType("metron_enrich").getFormat());
        Assert.assertEquals(HBASE_SIMPLE, config.getStorageForEnrichmentType("simple_enrich").getFormat());
    }

    @Test
    public void testLoadInvalidSyntaxFile() {
        String fullTestFilePath = getJsonAbsPath("invalid_syntax_enrichments_config.json");
        assertThatThrownBy(() -> testLoadJson(fullTestFilePath)).isInstanceOf(RuntimeException.class)
                .hasMessage(EnrichmentsConfig.ENRICHMENT_CONFIG_FILE_DESERIALIZATION_ERROR, fullTestFilePath)
                .hasCauseInstanceOf(com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class);
    }

    @Test
    public void testLoadInvalidSemanticsFile() {
        String fullTestFilePath = getJsonAbsPath("invalid_semantics_enrichments_config.json");
        assertThatThrownBy(() -> testLoadJson(fullTestFilePath)).isInstanceOf(RuntimeException.class)
                .hasMessage(EnrichmentsConfig.ENRICHMENT_CONFIG_FILE_DESERIALIZATION_ERROR, fullTestFilePath)
                .hasCause(new IllegalStateException(String.format(EnrichmentsConfig.MISSING_STORAGE_ERROR, DEFAULT_ENRICHMENT_STORAGE_NAME)));
    }

    @Test
    public void testLoadFileDoesNotExist() {
        String doesNotExistFile = "file_does_not_exist.json";
        assertThatThrownBy(() -> testLoadJson(doesNotExistFile)).isInstanceOf(RuntimeException.class).
                hasMessage(EnrichmentsConfig.ENRICHMENT_CONFIG_FILE_DESERIALIZATION_ERROR, doesNotExistFile)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    public void testGetTableNames() {
        EnrichmentsConfig config = new EnrichmentsConfig();
        String table1 = "table_1";
        String table2 = "table_2";
        String unreferencedTable = "unreferenced_table";
        String simpleFormat = "simple";
        String metronCf2Format = "metron_cf2";
        config.getStorageConfigs().put(DEFAULT_ENRICHMENT_STORAGE_NAME, new EnrichmentStorageConfig(HBASE_METRON, table1, "cf"));
        config.getStorageConfigs().put(metronCf2Format, new EnrichmentStorageConfig(HBASE_METRON, table1, "cf2"));
        config.getStorageConfigs().put(simpleFormat, new EnrichmentStorageConfig(HBASE_SIMPLE, table2, null));
        config.getStorageConfigs().put("not_referenced", new EnrichmentStorageConfig(HBASE_SIMPLE, unreferencedTable, null ));

        String enrichmentType1 = "type_1";
        String enrichmentType2 = "type_2";
        String enrichmentType3 = "type_3";
        String enrichmentType4 = "type_4";
        Map<String, EnrichmentConfig> enrichmentConfigs = config.getEnrichmentConfigs();
        enrichmentConfigs.put(enrichmentType1, createEnrichmentConfig("default", KEY_FIELDS, null, null));
        enrichmentConfigs.put(enrichmentType2, createEnrichmentConfig(simpleFormat, KEY_FIELDS, null, null));
        enrichmentConfigs.put(enrichmentType3, createEnrichmentConfig(simpleFormat,  KEY_FIELDS, null, null));
        enrichmentConfigs.put(enrichmentType4, createEnrichmentConfig(metronCf2Format,KEY_FIELDS, null, null));

        List<String> referencedTables = config.getReferencedTables();
        Assert.assertEquals(Lists.newArrayList(table1, table2), referencedTables);
    }

    @Test
    public void testGetStreamingSources() {
        String firstSource = "first_source";
        String secondSource = "second_source";
        String duplicateSource = "duplicate_source";
        EnrichmentsConfig config = new EnrichmentsConfig();
        config.getStorageConfigs().put(DEFAULT_ENRICHMENT_STORAGE_NAME, new EnrichmentStorageConfig(HBASE_METRON, "enrichments", "cf"));
        config.getEnrichmentConfigs().put("not_streaming", new EnrichmentConfig(null, new EnrichmentFieldsConfig(KEY_FIELDS, null, null, null)));

        Assert.assertTrue(config.getStreamingEnrichmentSources().isEmpty());

        config.getEnrichmentConfigs().put("et1", new EnrichmentConfig(null, new EnrichmentFieldsConfig(KEY_FIELDS, null, null, Lists.newArrayList(firstSource))));
        config.getEnrichmentConfigs().put("et2", new EnrichmentConfig(null, new EnrichmentFieldsConfig(KEY_FIELDS, null, null, Lists.newArrayList(secondSource, duplicateSource))));
        config.getEnrichmentConfigs().put("duplicate", new EnrichmentConfig(null, new EnrichmentFieldsConfig(KEY_FIELDS, null, null, Lists.newArrayList(duplicateSource))));

        Assert.assertEquals(Lists.newArrayList(secondSource, duplicateSource, firstSource), config.getStreamingEnrichmentSources());
    }

    @Test
    public void testLoadStreamingConfig() {
        EnrichmentsConfig config = testLoadJson(getJsonAbsPath("streaming_enrichments_config.json"));
        List<String> streamingEnrichmentSources = config.getStreamingEnrichmentSources();
        Assert.assertEquals(Lists.newArrayList("malicious_domain"), streamingEnrichmentSources);
        List<String> tables = config.getReferencedTablesForSource(streamingEnrichmentSources.get(0));
        Assert.assertEquals(Lists.newArrayList("simple_enrich"), tables);
    }

    private EnrichmentsConfig testLoadJson(String fullTestPath) {
        return EnrichmentsConfig.load(fullTestPath);
    }

    private String getJsonAbsPath(String testFileName) {
        File file = new File("src/test/resources/".concat(testFileName));
        return file.getAbsolutePath();
    }
    private EnrichmentsConfig createValidEnrichmentsConfig() {
        EnrichmentsConfig config = new EnrichmentsConfig();
        config.getEnrichmentConfigs().put("valid_enrich", createEnrichmentConfig("default", KEY_FIELDS, null, null));
        config.getStorageConfigs().put(DEFAULT_ENRICHMENT_STORAGE_NAME, DEFAULT_STORAGE);
        return config;
    }

    private EnrichmentConfig  createEnrichmentConfig(String storage,
                                                     ArrayList<String> keyFields, String keyDelimiter, ArrayList<String> valueFields)  {
        return new EnrichmentConfig(storage,
                new EnrichmentFieldsConfig(keyFields, keyDelimiter, valueFields, null));
    }

}
