package com.cloudera.cyber.indexing.hive.tableapi.impl;

import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.TableDto;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiAbstractJob;
import com.cloudera.cyber.indexing.hive.util.AvroSchemaUtil;
import com.cloudera.cyber.indexing.hive.util.FlinkSchemaUtil;
import com.cloudera.cyber.scoring.ScoredMessage;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.FormatDescriptor;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.types.Row;

public class TableApiKafkaJob extends TableApiAbstractJob {

  private static final String BASE_TABLE_JSON = "base-hive-table.json";

  public TableApiKafkaJob(ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source)
      throws IOException {
    super(params, env, source, "Kafka", BASE_TABLE_JSON, FlinkSchemaUtil.SerializationFormat.AVRO);
  }

  @Override
  protected ResolvedSchema createTable(StreamTableEnvironment tableEnv, String tableName,
      List<TableColumnDto> columnList) {
    return FlinkSchemaUtil.getResolvedSchema(columnList, serializationFormat);
  }

  @Override
  protected void createInserts(StreamTableEnvironment tableEnv, Map<String, MappingDto> topicMapping,
                               Map<String, TableDto> tablesConfig, Map<String, MappingDto> unenrichedTopicMapping, Map<String, ResolvedSchema> tableSchemaMap) {
    topicMapping.forEach((source, mappingDto) -> {
      registerFilterMapFunction(tableEnv, source, mappingDto, Collections.emptySet());
      final String insertSql = buildInsertSql(source, mappingDto, tableSchemaMap.get(mappingDto.getTableName()));
      try {
        //create view
        tableEnv.executeSql(insertSql);

        //read from view and write to kafka sink
        final Table table = tableEnv.from(getTableName(source, mappingDto));
        final DataStream<Row> tableStream = tableEnv.toDataStream(table);

        outputViewToKafka(tablesConfig, mappingDto, tableStream, mappingDto.getTableName());
        Optional.ofNullable(unenrichedTopicMapping.get(source))
                .ifPresent(unenrichedDto -> outputViewToKafka(tablesConfig, unenrichedDto, tableStream, unenrichedDto.getTableName(), getFilterMapIgnorePrefixes(source)));

        System.out.printf("Insert SQL added to the queue for the table: %s%nSQL: %s%n", mappingDto.getTableName(),
            insertSql);
      } catch (Exception e) {
        System.err.printf("Error adding insert to the statement set: %s%n", insertSql);
        throw e;
      }
    });
  }

  private void outputViewToKafka(Map<String, TableDto> tablesConfig, MappingDto mappingDto, DataStream<Row> tableStream, String topicName){
    outputViewToKafka(tablesConfig, mappingDto, tableStream, topicName, null);
  }

  private void outputViewToKafka(Map<String, TableDto> tablesConfig, MappingDto mappingDto, DataStream<Row> tableStream, String topicName, Set<String> filterPrefixes) {
    final String schemaString = AvroSchemaUtil.convertToAvro(tablesConfig.get(mappingDto.getTableName()).getColumns())
        .toString();

    System.out.printf("\nOutputting to topic: %s\n", topicName);
    System.out.printf("Schema: %s\n\n", schemaString);
    System.out.printf("Filtered Fields: %s\n", mappingDto.getIgnoreFields());
    System.out.printf("Filtered Prefixes: %s\n", filterPrefixes != null ? filterPrefixes : "none");

    final DataStream<GenericRecord> stream = tableStream.map(new MapRowToAvro(schemaString, filterPrefixes));
    final KafkaSink<GenericRecord> kafkaSink = new FlinkUtils<>(GenericRecord.class).createKafkaSink(
            topicName, "indexing-job", params);
    stream.sinkTo(kafkaSink);
  }

  @Override
  protected HashSet<String> getExistingTableList(StreamTableEnvironment tableEnv) {
    //Kafka tables are temporary, so no tables are present on the job creation
    return new HashSet<>();
  }

  @Override
  protected void registerCatalog(StreamTableEnvironment tableEnv) {

  }

  @Override
  protected String getTableConnector() {
    return "filesystem";
  }

  @Override
  protected FormatDescriptor getFormatDescriptor() {
    return null;
  }

  @Override
  protected String getTableName(String source, MappingDto mappingDto) {
    return source.concat("_tmpview");
  }

  @Override
  protected String getInsertSqlPrefix() {
    return "CREATE TEMPORARY VIEW ";
  }

  @Override
  protected String getInsertSqlSuffix() {
    return " AS ";
  }
}
