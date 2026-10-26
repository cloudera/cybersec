package com.cloudera.cyber.indexing.hive.tableapi;

import org.apache.flink.api.java.utils.ParameterTool;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexTableMappingConfigurationTest {

    private static final String BASE_TABLE_JSON = "base-hive-table.json";

    @TempDir
    Path tempDir;

    @Test
    void testReadConfigurationWithTriageParams() throws Exception {
        IndexTableMappingConfiguration configuration = readConfiguration(true, "index-tables.json", triageParams());

        // default columns are merged into every topic mapping, including those without a column mapping
        assertTrue(hasColumn(configuration, "squid", "dt"));
        assertTrue(hasColumn(configuration, "test", "id"));

        // a table with non-enriched columns produces an unenriched table and mapping
        assertTrue(configuration.getTablesConfig().containsKey("examples_full_events_raw"));
        assertTrue(configuration.getUnenrichedTopicMapping().containsKey("squid"));

        // a table whose columns are all enriched produces no unenriched table
        assertFalse(configuration.getTablesConfig().containsKey("enriched_only_table_raw"));

        // a table without a column definition still receives the default and partition columns
        assertFalse(configuration.getTablesConfig().get("no_columns_table").getColumns().isEmpty());
    }

    @Test
    void testReadConfigurationReadsTriageConfigFromIndexParams() throws Exception {
        Path triageFile = tempDir.resolve("triage.properties");
        Files.writeString(triageFile, String.join("\n",
                "geo.enabled=false",
                "asn.enabled=false",
                "company.enabled=false",
                "lookups.config.file=" + resourcePath("index-lookups.json")));

        Map<String, String> params = indexParams(true, "index-tables.json");
        params.put("triage.config", triageFile.toString());

        IndexTableMappingConfiguration configuration = new IndexTableMappingConfiguration(BASE_TABLE_JSON);
        configuration.readConfiguration(ParameterTool.fromMap(params), null);

        assertTrue(configuration.getUnenrichedTopicMapping().containsKey("squid"));
    }

    @Test
    void testReadConfigurationWithoutTriage() throws Exception {
        IndexTableMappingConfiguration configuration =
                readConfiguration(true, "index-tables-no-unenriched.json", null);

        assertTrue(configuration.getUnenrichedTopicMapping().isEmpty());
        assertTrue(configuration.getTablesConfig().containsKey("plain_table"));
    }

    @Test
    void testReadConfigurationWithoutTriageRejectsUnenrichedTables() {
        IndexTableMappingConfiguration configuration = newIndexTableMappingConfiguration();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> configuration.readConfiguration(ParameterTool.fromMap(indexParams(true, "index-tables.json")), null));

        assertTrue(exception.getMessage().contains("no triage config property is specified"));
    }

    @Test
    void testReadConfigurationWithoutTablesFile() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("flink.mapping-file", resourcePath("index-mapping.json"));

        IndexTableMappingConfiguration configuration = new IndexTableMappingConfiguration(BASE_TABLE_JSON);
        configuration.readConfiguration(ParameterTool.fromMap(params), null);

        assertTrue(configuration.getTablesConfig().isEmpty());
    }

    @Test
    void testAppendDefaultTablesConfigWithNullConfig() throws Exception {
        IndexTableMappingConfiguration configuration = newIndexTableMappingConfiguration();

        // readConfiguration never passes a null config, so the defensive null branch is exercised directly
        Method appendDefaults = IndexTableMappingConfiguration.class
                .getDeclaredMethod("appendDefaultTablesConfig", Map.class);
        appendDefaults.setAccessible(true);

        assertTrue(((Map<?, ?>) appendDefaults.invoke(configuration, (Object) null)).isEmpty());
    }

    @Test
    void testIsNonDefaultColumn() {
        IndexTableMappingConfiguration configuration = newIndexTableMappingConfiguration();

        assertFalse(configuration.isNonDefaultColumn("id"));
        assertTrue(configuration.isNonDefaultColumn("custom_column"));
    }

    private IndexTableMappingConfiguration readConfiguration(boolean includeTables, String tablesFile,
                                                             ParameterTool triageParams) throws Exception {
        IndexTableMappingConfiguration configuration = newIndexTableMappingConfiguration();
        configuration.readConfiguration(ParameterTool.fromMap(indexParams(includeTables, tablesFile)), triageParams);
        return configuration;
    }

    private IndexTableMappingConfiguration newIndexTableMappingConfiguration() {
        try {
            return new IndexTableMappingConfiguration(BASE_TABLE_JSON);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private boolean hasColumn(IndexTableMappingConfiguration configuration, String topic, String columnName) {
        return configuration.getTopicMapping().get(topic).getColumnMapping().stream()
                .anyMatch(column -> column.getName().equals(columnName));
    }

    private Map<String, String> indexParams(boolean includeTables, String tablesFile) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("flink.mapping-file", resourcePath("index-mapping.json"));
        if (includeTables) {
            params.put("flink.tables-init-file", resourcePath(tablesFile));
        }
        return params;
    }

    private ParameterTool triageParams() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("geo.enabled", "false");
        params.put("asn.enabled", "false");
        params.put("company.enabled", "false");
        params.put("lookups.config.file", resourcePath("index-lookups.json"));
        return ParameterTool.fromMap(params);
    }

    private String resourcePath(String name) throws Exception {
        return new File(getClass().getClassLoader().getResource(name).toURI()).getAbsolutePath();
    }
}
