package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloudera.cyber.parser.ParserJob.PARAM_PRIVATE_KEY;
import static com.cloudera.cyber.parser.ParserJob.PARAM_PRIVATE_KEY_FILE;

@Slf4j
public abstract class SplitJob {

    private static final String PARAMS_CONFIG_JSON = "config.json";
    protected String configJson;

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        List<SplitConfig> configs = parseConfig();
        Map<String, SplitConfig> configMap = configs.stream().collect(Collectors.toMap(k -> k.getTopic(), v->v));

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<MessageToParse> source = createSource(env, params, configMap.keySet());

        BroadcastStream<SplitConfig> configStream = createConfigSource(env, params)
                        .broadcast(Descriptors.broadcastState);

        byte[] privKeyBytes = params.has(PARAM_PRIVATE_KEY_FILE) ?
                Files.readAllBytes(Paths.get(params.get(PARAM_PRIVATE_KEY_FILE))) :
                Base64.getDecoder().decode(params.getRequired(PARAM_PRIVATE_KEY));

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privKeyBytes);
        PrivateKey signKey = keyFactory.generatePrivate(privSpec);

        SingleOutputStreamOperator<Message> results = source
                .keyBy(k -> k.getTopic())
                .connect(configStream)
                .process(new SplitBroadcastProcessFunction(configMap, signKey));

        writeOriginalsResults(params, source);

        SingleOutputStreamOperator<Message> parsed = results.map(new ParserChainMapFunction(configMap));

        writeResults(params, parsed);

        return env;
    }

    protected static class Descriptors {
        public static MapStateDescriptor<String, SplitConfig> broadcastState = new MapStateDescriptor<String, SplitConfig>("configs", String.class, SplitConfig.class);
    }

    protected List<SplitConfig> parseConfig() throws IllegalArgumentException, IOException {
        try {
            return JSONUtils.INSTANCE.getMapper().readValue(configJson, new TypeReference<List<SplitConfig>>() {});
        } catch (Exception e) {
            log.error(String.format("Failed to read split config %s", configJson));
            throw new IllegalArgumentException("Config could not be read for splits", e);
        }
    }

    protected abstract DataStream<SplitConfig> createConfigSource(StreamExecutionEnvironment env, ParameterTool params);
    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);
    protected abstract void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results);
    protected abstract DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params, Iterable<String> topics);
}
