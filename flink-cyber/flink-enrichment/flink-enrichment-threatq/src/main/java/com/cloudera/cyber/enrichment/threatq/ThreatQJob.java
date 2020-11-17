package com.cloudera.cyber.enrichment.threatq;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;

public abstract class ThreatQJob {
    public static DataStream<Message> enrich(DataStream<ThreatQEntry> threatQSource,
                                             DataStream<Message> source,
                                             List<ThreatQConfig> configs
    ) {

        DataStream<ThreatQEntry> enrichmentSource = threatQSource;
        DataStream<Message> pipeline = source;

        return pipeline;
    }

    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);
        DataStream<ThreatQEntry> enrichmentSource = createEnrichmentSource(env, params);

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));

        DataStream<Message> pipeline = enrich(enrichmentSource, source, allConfigs(configJson));
        writeResults(env, params, pipeline);
        return env;
    }

    protected List<ThreatQConfig> allConfigs(byte[] configJson) {
        return Collections.emptyList();
    }

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction);

    public abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<ThreatQEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);

}
