package com.cloudera.cyber.indexing.hive.tableapi.impl;

import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiAbstractJob;
import com.cloudera.cyber.indexing.hive.util.AvroSchemaUtil;
import com.cloudera.cyber.scoring.ScoredMessage;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.table.api.FormatDescriptor;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TableApiKafkaJob extends TableApiAbstractJob {

    private static final String BASE_TABLE_JSON = "base-hive-table.json";

    public TableApiKafkaJob(ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source) throws IOException {
        super(params, env, source, "Kafka", BASE_TABLE_JSON);
    }

    @Override
    protected void createTableIfNotExists(StreamTableEnvironment tableEnv, Set<String> tableList, String tableName, List<TableColumnDto> columnList) {

    }

    @Override
    protected void executeInsert(StreamTableEnvironment tableEnv, Map<String, MappingDto> topicMapping, Map<String, List<TableColumnDto>> tablesConfig) {
        topicMapping.forEach((topic, mappingDto) -> {
            final String insertSql = buildInsertSql(topic, mappingDto);
            try {
                //create view
                tableEnv.executeSql(insertSql);
                final KafkaSink<GenericRecord> kafkaSink = new FlinkUtils<>(GenericRecord.class).createKafkaSink(mappingDto.getTableName(), "indexing-job", params);

                //read from view and write to kafka sink
                final Table table = tableEnv.from(mappingDto.getTableName());
                final String schemaString = AvroSchemaUtil.convertToAvro(tablesConfig.get(mappingDto.getTableName())).toString();

                final DataStream<GenericRecord> stream = tableEnv.toDataStream(table).map(row -> {
                    final Schema schema = new Schema.Parser().parse(schemaString);
                    final GenericRecord record = new GenericData.Record(schema);
                    final Set<String> fieldNames = row.getFieldNames(true);
                    if (fieldNames != null) {
                        for (String fieldName : fieldNames) {
                            AvroSchemaUtil.putRowIntoAvro(row, record, fieldName);
                        }
                    }

                    return record;
                });
                stream.sinkTo(kafkaSink);
                System.out.printf("Insert SQL added to the queue for the table: %s%nSQL: %s%n", mappingDto.getTableName(), insertSql);
            } catch (Exception e) {
                System.err.printf("Error adding insert to the statement set: %s%n", insertSql);
                throw e;
            }
        });
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
    protected String getInsertSqlPrefix() {
        return "CREATE TEMPORARY VIEW ";
    }

    @Override
    protected String getInsertSqlSuffix() {
        return " AS ";
    }
}
