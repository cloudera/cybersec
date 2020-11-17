package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.catalog.exceptions.TableNotExistException;
import org.apache.flink.table.catalog.hive.HiveCatalog;
import org.apache.flink.table.descriptors.ConnectTableDescriptor;
import org.apache.flink.table.descriptors.Schema;
import org.apache.flink.types.Row;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class HiveJob {
    protected static final String PARAMS_HIVE_CONF = "hive.confdir";
    protected static final String DEFAULT_HIVE_CONF = "/etc/hive/conf/";
    protected static final String PARAMS_HIVE_TABLE = "hive.table";
    protected static final String DEFAULT_HIVE_TABLE = "events";
    protected static final String PARAMS_HIVE_CATALOG = "hive.catalog";
    protected static final String DEFAULT_HIVE_CATALOG = "default";
    protected static final String PARAMS_HIVE_SCHEMA = "hive.schema";
    protected static final String DEFAULT_HIVE_SCHEMA = "cyber";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws TableNotExistException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
        FlinkUtils.setupEnv(env, params);

        EnvironmentSettings settings = EnvironmentSettings.newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);

        String hiveConfDir = params.get(PARAMS_HIVE_CONF, DEFAULT_HIVE_CONF);
        String catalog = params.get(PARAMS_HIVE_CATALOG, DEFAULT_HIVE_CATALOG);
        String schema = params.get(PARAMS_HIVE_SCHEMA, DEFAULT_HIVE_SCHEMA);
        String table = params.get(PARAMS_HIVE_TABLE, DEFAULT_HIVE_TABLE);

        HiveCatalog hive = new HiveCatalog(catalog, schema, hiveConfDir, "3.1.2");
        tableEnv.registerCatalog("hive", hive);

        DataStream<Message> source = createSource(env, params);

        // get the schema of the hive table
        TableSchema tableSchema = hive.getTable(new ObjectPath(schema, table)).getSchema();
        List<String> fields = Arrays.asList(tableSchema.getFieldNames());
        FieldExtractor fieldExtractor = new FieldExtractor(fields);

        SingleOutputStreamOperator<Row> output = source
                // TODO - apply any filtering here
                // map the input messages to the hive schema
                .map(fieldExtractor)
                .returns(fieldExtractor.type());
        tableEnv.createTemporaryView("input", output);

        ConnectTableDescriptor outputTableDescriptor = tableEnv
                .connect(new HiveStreamingConnector(schema, table))
                .withSchema(new Schema().schema(tableSchema))
                .inAppendMode();

        outputTableDescriptor.createTemporaryTable("outputTable");

        String fieldNames = fieldExtractor.fieldNames().stream().collect(Collectors.joining(","));
        String sql = String.format("insert into outputTable (%s) select %s from input", fieldNames, fieldNames);
        // write the hive entry
        tableEnv.sqlUpdate(sql);

        return env;
    }

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
}