package com.cloudera.cyber.indexing.hive.tableapi.impl;

import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.indexing.MappingColumnDto;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiAbstractJob;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Streams;
import groovy.util.logging.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class TableApiIcebergJob extends TableApiAbstractJob {

    private static final String ICEBERG_TABLES_INIT_FILE_PARAM = "iceberg.tables-init-file";
    private static final String ICEBERG_MAPPING_FILE_PARAM = "iceberg.mapping-file";

    private static final String BASE_COLUMN_MAPPING_JSON = "base-column-mapping.json";
    private static final String BASE_TABLE_JSON = "base-hive-table.json";

    private final List<MappingColumnDto> defaultMappingList;
    private final List<TableColumnDto> defaultColumnList;

    public TableApiIcebergJob(ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source) throws IOException {
        super(params, env, source);
        defaultMappingList = Utils.readResourceFile(BASE_COLUMN_MAPPING_JSON, getClass(),
                new TypeReference<List<MappingColumnDto>>() {
                });
        defaultColumnList = Utils.readResourceFile(BASE_TABLE_JSON, getClass(),
                new TypeReference<List<TableColumnDto>>() {
                });
    }

    @Override
    protected Map<String, MappingDto> getTopicMapping() throws IOException {
        TypeReference<HashMap<String, MappingDto>> typeRef
                = new TypeReference<HashMap<String, MappingDto>>() {
        };
        final HashMap<String, MappingDto> columnMappingMap = Utils.readFile(params.getRequired(ICEBERG_MAPPING_FILE_PARAM), typeRef);

        //adding the default column mappings to each topic
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

    @Override
    protected void createTableIfNotExists(StreamTableEnvironment tableEnv, Set<String> tableList, String tableName, List<TableColumnDto> columnList) {
        if (tableList.contains(tableName)) {
            System.out.printf("Iceberg table [%s] already exists. Skipping its creation.%n", tableName);
        } else {
            System.out.printf("Creating Iceberg table %s...%n", tableName);
            final String ddl = buildIcebergTableDLL(tableName, columnList);
            try {
                tableEnv.executeSql(ddl);
            } catch (Exception e) {
                System.err.printf("Error executing the Iceberg DDL: %s%n", ddl);
                throw e;
            }
            System.out.printf("Iceberg table created: %s%n", tableName);
        }
    }


    @Override
    protected Map<String, List<TableColumnDto>> getTablesConfig() throws IOException {
        TypeReference<HashMap<String, List<TableColumnDto>>> typeRef
                = new TypeReference<HashMap<String, List<TableColumnDto>>>() {
        };
        final String filePath = params.get(ICEBERG_TABLES_INIT_FILE_PARAM);
        if (filePath == null) {
            return Collections.emptyMap();
        }
        final HashMap<String, List<TableColumnDto>> columnMap = Utils.readFile(filePath, typeRef);
        //adding the default columns to each table
        columnMap.forEach((tableName, columnList) -> {
            final List<TableColumnDto> customColumns = Optional.ofNullable(columnList)
                    .orElse(Collections.emptyList());
            final Collection<TableColumnDto> combinedColumns = Streams.concat(customColumns.stream(), defaultColumnList.stream())
                    .collect(Collectors.toMap(TableColumnDto::getName, Function.identity(), (f, s) -> f)).values();
            columnMap.put(tableName, new ArrayList<>(combinedColumns));
        });
        return columnMap;
    }

    @Override
    protected String getInsertColumns(MappingDto mappingDto) {
        return mappingDto.getColumnMapping().stream()
                .map(MappingColumnDto::getName)
                .collect(Collectors.joining("`, `", " `", "` "));
    }

    private String buildIcebergTableDLL(String tableName, List<TableColumnDto> columnList) {
        return String.join("\n", "CREATE TABLE IF NOT EXISTS " + tableName + " ( ",
                getColumnList(columnList),
                ", `dt` string, ",
                " `hr` string",
                ") PARTITIONED BY (",
                " `dt`, ",
                " `hr`",
                ")");
    }

    private String getColumnList(List<TableColumnDto> columnList) {
        return columnList.stream()
                .map(col -> String.format("`%s` %s %s",
                        col.getName(), col.getType(),
                        Optional.ofNullable(col.getNullable()).orElse(true) ? "" : "NOT NULL"))
                .collect(Collectors.joining(", \n", " ", " \n"));
    }


}
