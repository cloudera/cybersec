package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.catalog.exceptions.TableNotExistException;
import org.apache.flink.table.catalog.hive.HiveCatalog;
import org.apache.flink.types.Row;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Slf4j
public abstract class HiveJob {
    private static final String PARAMS_HIVE_TABLE = "hive.table";
    private static final String DEFAULT_HIVE_TABLE = "events";
    private static final String PARAMS_HIVE_CATALOG = "hive.catalog";
    private static final String DEFAULT_HIVE_CATALOG = "default";
    private static final String PARAMS_HIVE_SCHEMA = "hive.schema";
    private static final String DEFAULT_HIVE_SCHEMA = "cyber";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws TableNotExistException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        EnvironmentSettings settings = EnvironmentSettings.newInstance()
                .inStreamingMode()
                .useBlinkPlanner()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);

        String hiveConfDir = "/etc/hive/conf/";

        String catalog = params.get(PARAMS_HIVE_CATALOG, DEFAULT_HIVE_CATALOG);
        String schema = params.get(PARAMS_HIVE_SCHEMA, DEFAULT_HIVE_SCHEMA);
        String table = params.get(PARAMS_HIVE_TABLE, DEFAULT_HIVE_TABLE);

        HiveCatalog hive = new HiveCatalog(catalog, schema, hiveConfDir, "3.1.2");
        tableEnv.registerCatalog("hive", hive);
        tableEnv.useCatalog("hive");

        DataStream<Message> source = createSource(env, params);

        // get the schema of the hive table
        TableSchema schemaDetail = hive.getTable(new ObjectPath(schema, table)).getSchema();
        List<String> fields = Arrays.asList(schemaDetail.getFieldNames());
        FieldExtractor fieldExtractor = new FieldExtractor(fields);

        DataStream<Row> output = source
                // TODO - apply any filtering and index projection logic here
                // map the input messages to the hive schema
                .map(fieldExtractor).returns(fieldExtractor.type());

        // write the hive entry
        tableEnv.insertInto(table, tableEnv.fromDataStream(output));
        return env;
    }

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
}
