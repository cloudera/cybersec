package com.cloudera.cyber.enrichment.rest;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;
import static org.apache.commons.codec.digest.DigestUtils.md5;

public abstract class RestLookupJob {

    public static DataStream<Message> enrich(DataStream<Message> source, List<RestEnrichmentConfig> configs) {
        return configs.stream().reduce(source, (in, config) -> {
            AsyncHttpRequest asyncHttpRequest = new AsyncHttpRequest(config);
            String processId = "rest-" + md5(source + config.getEndpointTemplate());

            DataStream<Message> messages = in.filter(m -> m.getSource().equals(config.getSource()))
                    .name("Filter - " + config.getEndpointTemplate() + " " + config.getSource())
                    .uid("filter-" + processId);

            return AsyncDataStream.unorderedWait(
                    messages,
                    asyncHttpRequest, config.getTimeout(), TimeUnit.MILLISECONDS, config.getCapacity())
                    .name("REST - " + config.getEndpointTemplate() + " " + config.getSource())
                    .uid("rest-" + processId);
        }, (a, b) -> a); // TODO - does the combiner really make sense?);
    }

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));

        DataStream<Message> pipeline = enrich(source, parseConfigs(configJson));
        writeResults(env, params, pipeline);
        return env;
    }

    public static List<RestEnrichmentConfig> parseConfigs(byte[] configJson) throws IOException {
        return new ObjectMapper().readValue(
                configJson,
                new TypeReference<List<RestEnrichmentConfig>>() {
                });
    }

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results);

}
