package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cloudera.cyber.enrichment.ConfigUtils.*;

public abstract class HbaseJob {

    public static DataStream<Message> enrich(DataStream<Message> source, StreamExecutionEnvironment env, List<EnrichmentConfig> configs) {
        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build());

        Map<String, List<String>> typeToFields = typeToFields(configs, EnrichmentKind.HBASE);
        Set<String> enrichmentTypes = enrichmentTypes(configs, EnrichmentKind.HBASE);

        tableEnv.createTemporaryView("messages", source.map(m -> Row.of(m.toByteBuffer().array(), null /* TODO add the fields required for enrichment */)));

        // output table should return original message in avro encoded bytes and the map of additional fields
        Table results = tableEnv.sqlQuery("");

        return tableEnv.toAppendStream(results,
                Types.ROW(Types.OBJECT_ARRAY(Types.BYTE), Types.MAP(Types.STRING, Types.STRING)))
                .map(r ->
                        MessageUtils.addFields(
                                Message.getDecoder().decode((byte[]) r.getField(0)),
                                (Map<String, String>) r.getField(1)
                        )
                );
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

    protected abstract void createTable(StreamTableEnvironment tableEnvironment, ParameterTool params);
}
