package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.java.StreamTableEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;
import static com.cloudera.cyber.enrichment.ConfigUtils.allConfigs;

public abstract class HbaseJob {

    public static DataStream<Message> enrich(DataStream<Message> source, StreamExecutionEnvironment env, List<EnrichmentConfig> configs) throws IOException {
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build());

        tableEnv.createTemporaryView("messages", source);

        tableEnv.registerFunction("MAP_MERGE", new MapMergeFunction());
        tableEnv.registerFunction("MAP_PREFIX", new PrefixMapFunction());
        tableEnv.registerFunction("HBASE_ENRICHMENT", new HbaseEnrichmentFunction());
        String sql = HbaseSQL.buildSql(configs);

        //Table results = tableEnv.sqlQuery(sql);

        Table results = tableEnv.sqlQuery("select * from messages");

        System.out.println("Source Type");
        System.out.println(source.getType().toString());
        TableSchema schema = results.getSchema();
        System.out.println("Schema");
        System.out.println(schema.toString());

        System.out.println("Schema row data type");
        System.out.println(schema.toRowDataType().toString());

        System.out.println("Message Type");
        System.out.println(TypeInformation.of(Message.class).toString());
        return tableEnv.toAppendStream(results, Message.class);
        /*return tableEnv.toAppendStream(results, TypeInformation.of(Row.class)).map(r -> {
            // TODO - map Row to Message
            // rowToMessage(r)
            Message.builder().build()
        });*/
    }

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<Message> source = createSource(env, params);
        DataStream<EnrichmentEntry> enrichmentSource = createEnrichmentSource(env, params);

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));

        DataStream<Message> result = enrich(source, env, allConfigs(configJson));
        writeResults(env, params, result);
        writeEnrichments(env, params, enrichmentSource);

        return env;
    }

    protected abstract void writeEnrichments(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentEntry> enrichmentSource);

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> result);

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<EnrichmentEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);
}

