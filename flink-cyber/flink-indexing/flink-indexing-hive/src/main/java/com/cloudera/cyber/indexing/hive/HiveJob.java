package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.exceptions.TableNotExistException;
import org.apache.flink.table.descriptors.ConnectTableDescriptor;
import org.apache.flink.table.descriptors.Schema;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.thrift.TException;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public abstract class HiveJob {
    protected static final String PARAMS_HIVE_CONF = "hive.confdir";
    protected static final String DEFAULT_HIVE_CONF = "/etc/hive/conf/";
    protected static final String PARAMS_HIVE_CATALOG = "hive.catalog";
    protected static final String DEFAULT_HIVE_CATALOG = "hive";
    protected static final String PARAMS_HIVE_SCHEMA = "hive.schema";
    protected static final String DEFAULT_HIVE_SCHEMA = "cyber";
    protected static final String PARAMS_HIVE_TABLE = "hive.table";
    protected static final String DEFAULT_HIVE_TABLE = "events";
    protected static final String PARAMS_INCLUDE_FIELD_MAP = "hive.include.all";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws TableNotExistException, TException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);
        env.setRestartStrategy(RestartStrategies.noRestart());

        EnvironmentSettings settings = EnvironmentSettings.newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build();
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, settings);

        String hiveConfDir = params.get(PARAMS_HIVE_CONF, DEFAULT_HIVE_CONF);
        String catalog = params.get(PARAMS_HIVE_CATALOG, DEFAULT_HIVE_CATALOG);
        String schema = params.get(PARAMS_HIVE_SCHEMA, DEFAULT_HIVE_SCHEMA);
        String table = params.get(PARAMS_HIVE_TABLE, DEFAULT_HIVE_TABLE);

        TableSchema tableSchema = getHiveTable(hiveConfDir, catalog, schema, table);
        DataStream<Message> source = createSource(env, params);

        // get the schema of the hive table
        List<String> fields = Arrays.asList(tableSchema.getFieldNames());
        FieldExtractor fieldExtractor = new FieldExtractor(fields, params.getBoolean(PARAMS_INCLUDE_FIELD_MAP, false));

        SingleOutputStreamOperator<Row> output = source
                // TODO - apply any filtering here
                // map the input messages to the hive schema
                .map(fieldExtractor)
                .returns(fieldExtractor.type())
                .name("field extract").uid("field-extract");

        ConnectTableDescriptor outputTableDescriptor = tableEnv
                .connect(new HiveStreamingConnector(schema, table))
                .withSchema(new Schema().schema(tableSchema))
                .inAppendMode();
        outputTableDescriptor.createTemporaryTable("outputTable");

        String fieldSpec = fieldExtractor.fieldNames().stream().collect(Collectors.joining(", "));
        ///.replace("ts", "ts.rowtime");
        tableEnv.fromDataStream(output, fieldSpec)
                .insertInto("outputTable");
        return env;
    }

    private TableSchema getHiveTable(String hiveConfDir, String catalog, String schema, String table) throws TException {
        HiveConf hiveConf = createHiveConf("/etc/hive/conf/");
        HiveMetaStoreClient hiveMetaStoreClient = null;

        hiveMetaStoreClient = new HiveMetaStoreClient(hiveConf);
        List<FieldSchema> hiveFields = hiveMetaStoreClient.getSchema(schema, table);
        return hiveFields.stream().reduce(TableSchema.builder(),
                (build, field) -> build.field(field.getName(), hiveTypeToDataType(field.getType())),
                (a, b) -> a
        ).build();
    }


    public static DataType hiveTypeToDataType(String type) {
        switch (type.toLowerCase()) {
            case "timestamp":
            case "bigint":
                return DataTypes.BIGINT();
            case "map<string,string>":
                return DataTypes.MAP(DataTypes.STRING(), DataTypes.STRING());
            default:
                return DataTypes.STRING();
        }
    }

    private static HiveConf createHiveConf(@Nullable String hiveConfDir) {
        log.info("Setting hive conf dir as {}", hiveConfDir);
        org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration();
        hadoopConf.addResource("/etc/hive/conf/hive-site.xml");
        hadoopConf.addResource("/etc/hive/conf/core-site.xml");
        try {
            HiveConf.setHiveSiteLocation(hiveConfDir == null ? null : Paths.get(hiveConfDir, "hive-site.xml").toUri().toURL());
        } catch (MalformedURLException e) {
            log.error("Cannot load Hive Config", e);
        }
        HiveConf hiveConf = new HiveConf(hadoopConf, HiveConf.class);
        return hiveConf;
    }

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
}
