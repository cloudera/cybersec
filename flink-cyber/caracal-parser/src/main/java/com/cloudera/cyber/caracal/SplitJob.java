package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.parser.MessageToParse;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class SplitJob {

    private static final String PARAMS_CONFIG_JSON = "config.json";
    private static final String PARAM_CHECKPOINT_INTERVAL = "checkpoint.interval.ms";
    protected String configJson;

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        List<SplitConfig> configs = parseConfig();
        Map<String, SplitConfig> configMap = configs.stream().collect(Collectors.toMap(k -> k.getTopic(), v->v));

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<MessageToParse> source = createSource(env, params, configMap.keySet());

        BroadcastStream<SplitConfig> configStream =
                env.fromCollection(configs)
                        .broadcast(Descriptors.broadcastState);

        SingleOutputStreamOperator<Message> results = source
                .keyBy(k -> k.getTopic())
                .connect(configStream)
                .process(new SplitBroadcastProcessFunction(configMap));

        writeResults(params, results);

        return env;
    }

    protected static class Descriptors {
        public static MapStateDescriptor<String, SplitConfig> broadcastState = new MapStateDescriptor<String, SplitConfig>("configs", String.class, SplitConfig.class);
    }

    protected List<SplitConfig> parseConfig() throws IllegalArgumentException, IOException {
        try {
            return new ObjectMapper().readValue(configJson, new TypeReference<List<SplitConfig>>() {
            });
        } catch (Exception e) {
            log.error(String.format("Failed to read split config %s", configJson));
            throw new IllegalArgumentException("Config could not be read for splits", e);
        }
    }

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);

    protected abstract DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params, Iterable<String> topics);
}
