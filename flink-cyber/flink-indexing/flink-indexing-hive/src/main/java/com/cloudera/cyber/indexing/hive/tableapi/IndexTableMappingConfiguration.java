package com.cloudera.cyber.indexing.hive.tableapi;

import com.cloudera.cyber.enrichment.EnrichmentConfiguration;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.indexing.MappingColumnDto;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.TableDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Streams;
import lombok.Data;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.util.Preconditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class IndexTableMappingConfiguration {

    private static final String TRIAGE_CONFIG_PARAM = "triage.config";
    private static final String TABLES_INIT_FILE_PARAM = "flink.tables-init-file";
    private static final String MAPPING_FILE_PARAM = "flink.mapping-file";

    private static final String BASE_COLUMN_MAPPING_JSON = "base-column-mapping.json";

    private final List<MappingColumnDto> defaultMappingList;
    private final List<TableColumnDto> defaultColumnList;
    private Map<String, MappingDto> topicMapping;
    private Map<String, TableDto> tablesConfig;
    private Map<String, MappingDto> unenrichedTopicMapping;
    private final String baseTableJson;

    public IndexTableMappingConfiguration(String baseTableJson) throws IOException {
        this.baseTableJson = baseTableJson;
        this.defaultMappingList = Utils.readResourceFile(BASE_COLUMN_MAPPING_JSON, getClass(),
                new TypeReference<List<MappingColumnDto>>() {
                });
        this.defaultColumnList = Utils.readResourceFile(baseTableJson, getClass(),
                new TypeReference<List<TableColumnDto>>() {
                });
    }

    public void readConfiguration(ParameterTool indexParams, ParameterTool triageParams) throws IOException {

        System.out.println("Getting topic mapping...");
        this.topicMapping = getTopicMapping(indexParams);

        System.out.println("Getting tables config...");
        final Map<String, TableDto> rawTablesConfig = getRawTablesConfig(indexParams);
        final Map<String, TableDto> rawUnenrichedTablesConfig = new HashMap<>();
        unenrichedTopicMapping = new HashMap<>();

        if (triageParams == null && !indexParams.has(TRIAGE_CONFIG_PARAM)) {
            System.out.printf("No triage config file property %s specified.  Skipping unenriched tables", TRIAGE_CONFIG_PARAM);
            Preconditions.checkState(rawTablesConfig.values().stream().noneMatch(dto -> (dto.getUnenrichedTableName() != null)),
                    "Table json contains unenriched tables but no triage config property is specified.");
        } else {
            // read the triage config, if we haven't already
            if (triageParams == null) {
                String triageConfigPath = indexParams.getRequired(TRIAGE_CONFIG_PARAM);
                System.out.printf("Reading triage config file '%s'...\n", triageConfigPath);
                triageParams = Utils.getParamToolsFromProperties(new String[]{triageConfigPath});
            }

            System.out.println("Getting enrichment field prefixes...");
            List<String> enrichmentPrefixes = EnrichmentConfiguration.getEnrichmentFieldPrefixes(triageParams);
            Map<String, Set<String>> sourceToEnrichmentPrefixes = EnrichmentConfiguration.getSourceEnrichmentFieldPrefixes(triageParams);
            System.out.printf("Filtered field prefixes: %s\n", String.join(", ", enrichmentPrefixes));
            System.out.println("Filtered source field prefixes:");
            sourceToEnrichmentPrefixes.forEach((source, sourceFilteredPrefixes) -> System.out.printf("%s: %s\n", source, String.join(", ", sourceFilteredPrefixes)));

            System.out.println("Getting unenriched tables config...");
            createDerivedTablesAndMappings(rawTablesConfig, topicMapping,
                    rawUnenrichedTablesConfig, unenrichedTopicMapping, enrichmentPrefixes, sourceToEnrichmentPrefixes);
        }

        tablesConfig = appendDefaultTablesConfig(rawTablesConfig);
        tablesConfig.putAll(appendDefaultTablesConfig(rawUnenrichedTablesConfig));
    }

    public boolean isNonDefaultColumn(String columnName) {
        return defaultColumnList.stream().noneMatch(c -> c.getName().equals(columnName));
    }

    private Map<String, MappingDto> getTopicMapping(ParameterTool params) throws IOException {
        TypeReference<HashMap<String, MappingDto>> typeRef
                = new TypeReference<HashMap<String, MappingDto>>() {
        };
        final HashMap<String, MappingDto> columnMappingMap = Utils.readFile(params.getRequired(MAPPING_FILE_PARAM),
                typeRef);

        // adding the default column mappings to each topic
        columnMappingMap.values().forEach(mapping -> {
            final List<MappingColumnDto> customMappings = Optional.ofNullable(mapping.getColumnMapping())
                    .orElse(Collections.emptyList());
            final Collection<MappingColumnDto> columnMapping = Streams.concat(
                            customMappings.stream(),
                            defaultMappingList.stream())
                    .collect(Collectors.toMap(MappingColumnDto::getName, Function.identity(), (f, s) -> f)).values();

            mapping.setColumnMapping(new ArrayList<>(columnMapping));
        });
        return columnMappingMap;
    }

    /**
     * Method provides contents of the tables config file.
     *
     * @return Map with table name as a key, and a list of columns as a value.
     * @throws IOException in case it can't read the tables config file.
     */
    private Map<String, TableDto> getRawTablesConfig(ParameterTool params) throws IOException {
        TypeReference<HashMap<String, TableDto>> typeRef
                = new TypeReference<HashMap<String, TableDto>>() {
        };
        final String filePath = params.get(TABLES_INIT_FILE_PARAM);
        if (filePath == null) {
            return Collections.emptyMap();
        }
        return Utils.readFile(filePath, typeRef);
    }

    private void createDerivedTablesAndMappings(Map<String, TableDto> tables, Map<String, MappingDto> topicMapping,
                                                  Map<String, TableDto> unenrichedTables, Map<String, MappingDto> unenrichedTopicMapping,
                                                  List<String> enrichmentPrefixes, Map<String, Set<String>> sourceToEnrichmentPrefixes) {
        // derive a set of unenriched tables
        tables.forEach((originalTableName, tableDto) -> {
            if (tableDto.getUnenrichedTableName() != null) {
                TableDto unenrichedTable = tableDto.deriveUnenrichedTable(originalTableName, topicMapping, unenrichedTopicMapping, enrichmentPrefixes, sourceToEnrichmentPrefixes);
                if (unenrichedTable != null) {
                    unenrichedTables.put(tableDto.getUnenrichedTableName(), unenrichedTable);
                }
            }
        });

    }
    /**
     * Method appends default and partition columns to the raw tables config.
     *
     * @return Map with table name as a key, and a list of columns as a value.
     */
    private Map<String, TableDto> appendDefaultTablesConfig(
            Map<String, TableDto> rawTablesConfig) {
        if (rawTablesConfig == null || rawTablesConfig.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, TableDto> columnMap = new HashMap<>(rawTablesConfig);
        final List<TableColumnDto> partitionColumns = Arrays.asList(TableColumnDto.builder()
                .name("dt")
                .type("string")
                .build(), TableColumnDto.builder()
                .name("hr")
                .type("string")
                .build());
        //adding the default columns to each table
        columnMap.forEach((tableName, tableDto) -> {
            final List<TableColumnDto> customColumns = Optional.ofNullable(tableDto.getColumns())
                    .orElse(Collections.emptyList());
            final Map<String, TableColumnDto> combinedColumnMap = Streams.concat(customColumns.stream(),
                            defaultColumnList.stream(), partitionColumns.stream())
                    .collect(Collectors.toMap(TableColumnDto::getName, Function.identity(), (f, s) -> f, LinkedHashMap::new));
            //partition columns should be placed last
            partitionColumns.forEach(col -> combinedColumnMap.put(col.getName(), col));

            columnMap.put(tableName, new TableDto(tableDto.getUnenrichedTableName(), new ArrayList<>(combinedColumnMap.values())));
        });
        return columnMap;
    }
}
