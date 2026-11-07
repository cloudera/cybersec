package com.cloudera.cyber.ondemand.source.decode;

import com.cloudera.cyber.indexing.MappingColumnDto;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.TableDto;
import com.cloudera.cyber.ondemand.config.OndemandEnrichmentConfiguration;
import org.apache.flink.util.Preconditions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ReadTableSql {

    private static final String SQL_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final DateTimeFormatter SQL_DATE_TIME_FORMATTER =  DateTimeFormatter.ofPattern(SQL_DATE_TIME_FORMAT);

    private static final String SQL_DATE_PARTITION_FORMAT = "yyyy-MM-dd";
    private static final DateTimeFormatter SQL_DATE_PARTITION_FORMATTER =  DateTimeFormatter.ofPattern(SQL_DATE_PARTITION_FORMAT);

    private static final String SQL_HOUR_PARTITION_FORMAT = "HH";
    private static final DateTimeFormatter SQL_HOUR_PARTITION_FORMATTER =  DateTimeFormatter.ofPattern(SQL_HOUR_PARTITION_FORMAT);


    public static List<String> generateSelectStatements(OndemandEnrichmentConfiguration configuration, String catalogName, String databaseName) {
        Map<String, MappingDto> unenrichedTopicMapping = configuration.indexTableMappingConfiguration().getUnenrichedTopicMapping();
        List<String> sources = configuration.sourcesToEnrich();
        // map unenriched table name -> list of sources contained in table
        Map<String, List<String>> tableToSources = new HashMap<>();
        unenrichedTopicMapping.entrySet().stream().
                filter(e -> sources.contains(e.getKey()) ).
                forEach(e -> tableToSources.computeIfAbsent(e.getValue().getTableName(), v -> new ArrayList<>()).add(e.getKey()));

        // generate one statement for each table
        return tableToSources.entrySet().stream().map((e) -> generateTableSelectStatement(configuration, catalogName, databaseName, e.getKey(), e.getValue())).toList();

    }

    private static String generateTableSelectStatement(OndemandEnrichmentConfiguration configuration,
                                                       String catalogName, String databaseName, String tableName,
                                                       List<String> sources) {
        Map<String, MappingDto> unenrichedMappings = configuration.indexTableMappingConfiguration().getUnenrichedTopicMapping();

        // get the mappings from the first source - all sources should map message elements to a common schema
        MappingDto unenrichedMappingDto = unenrichedMappings.get(sources.get(0));
        List<MappingColumnDto> columnMapping = unenrichedMappingDto.getColumnMapping();
        TableDto tableDto = configuration.indexTableMappingConfiguration().getTablesConfig().get(tableName);
        Preconditions.checkNotNull(tableDto);

        String tsColumnName = getColumnName(columnMapping, ReadTableSql::isTimestampColumn, "Timestamp", sources);
        String datePartitionColumnName = getColumnName(columnMapping, ReadTableSql::isDatePartitionColumn, "Date partition", sources);
        String hourPartitionColumnName = getColumnName(columnMapping, ReadTableSql::isHourPartitionColumn, "Hour partition", sources);
        String sourceColumnName = getColumnName(columnMapping, ReadTableSql::isSourceColumn, "Source", sources);

        return String.format("select %s from `%s`.`%s`.`%s` where %s",
                getSelectColumns(tableDto, datePartitionColumnName, hourPartitionColumnName), catalogName,
                databaseName, tableName,
                getWhereClause(configuration, sources, tsColumnName, datePartitionColumnName, hourPartitionColumnName, sourceColumnName));
    }

    private static String getColumnName(List<MappingColumnDto> columnMapping, Predicate<MappingColumnDto> predicate, String columnErrorName, List<String> sources) {
        MappingColumnDto mappingColumnDto = columnMapping.stream().filter(predicate).findFirst().
                orElseThrow(() -> new RuntimeException(String.format("%s column is not mapped for sources %s", columnErrorName, String.join(",", sources))));
        return mappingColumnDto.getName();
    }

    private static String getSelectColumns(TableDto tableDto, String datePartitionColumn, String hourPartitionColumn) {
               return tableDto.getColumns().stream().map(TableColumnDto::getName).filter(columnName -> !columnName.toLowerCase().contains("score") &&
                       !columnName.equals(hourPartitionColumn) && !columnName.equals(datePartitionColumn)).collect(Collectors.joining(", "));
    }

    private static boolean isTimestampColumn(MappingColumnDto mappingColumnDto) {
        return (!mappingColumnDto.getIsMap() && mappingColumnDto.getPath().isEmpty() && mappingColumnDto.getProperName().equals("ts") &&
                (mappingColumnDto.getTransformation() == null || !mappingColumnDto.getTransformation().contains("DATE_FORMAT")));
    }

    private static boolean isDatePartitionColumn(MappingColumnDto mappingColumnDto) {
        return (!mappingColumnDto.getIsMap() && mappingColumnDto.getPath().isEmpty() && mappingColumnDto.getProperName().equals("ts") &&
                mappingColumnDto.getTransformation().contains("DATE_FORMAT") &&
                mappingColumnDto.getTransformation().contains("'yyyy-MM-dd'"));
    }
    private static boolean isHourPartitionColumn(MappingColumnDto mappingColumnDto) {
        return (!mappingColumnDto.getIsMap() && mappingColumnDto.getPath().isEmpty() && mappingColumnDto.getProperName().equals("ts") &&
                mappingColumnDto.getTransformation().contains("DATE_FORMAT") &&
                mappingColumnDto.getTransformation().contains("'HH'"));
    }

    private static boolean isSourceColumn(MappingColumnDto mappingColumnDto) {
        return (!mappingColumnDto.getIsMap() && mappingColumnDto.getPath().isEmpty() && mappingColumnDto.getProperName().equals("source"));
    }


    private static String getWhereClause(OndemandEnrichmentConfiguration configuration, List<String> sources,
                                         String tsColumnName, String datePartitionColumnName, String hourPartitionColumnName,
                                         String sourceColumnName) {

        String commaSeparatedSources = sources.stream().map(ReadTableSql::quote).collect(Collectors.joining(","));
        return String.format("%s and %s and %s and %s and %s in ( %s ) ",
                checkPartition(configuration.startTime(), datePartitionColumnName, hourPartitionColumnName, ">"),
                checkPartition(configuration.endTime(), datePartitionColumnName, hourPartitionColumnName, "<"),
                getTimePredicate(configuration.startTime(), tsColumnName, ">="),
                getTimePredicate(configuration.endTime(), tsColumnName,"<="),
                sourceColumnName, commaSeparatedSources);

    }

    private static String getTimePredicate(LocalDateTime dateTime, String tsColumnName, String comparisonOperator) {
        return String.join(" ", tsColumnName, comparisonOperator, "TIMESTAMP", quote(SQL_DATE_TIME_FORMATTER.format(dateTime)));
    }

    private static String checkPartition(LocalDateTime dateTime, String datePartitionColumnName, String hourPartitionColumnName,
                                         String comparison) {

        String datePartition  = SQL_DATE_PARTITION_FORMATTER.format(dateTime);
        String hourPartition = SQL_HOUR_PARTITION_FORMATTER.format(dateTime);

        return String.format("(%s %s '%s' OR (%s = '%s' AND %s %s= '%s'))", datePartitionColumnName, comparison, datePartition,
                datePartitionColumnName, datePartition, hourPartitionColumnName, comparison, hourPartition);
    }

    private static String quote(String originalString) {
        return String.format("'%s'", originalString);
    }
}
