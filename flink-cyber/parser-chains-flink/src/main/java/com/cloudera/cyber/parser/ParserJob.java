package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.parserchains.core.utils.JSONUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import org.apache.kafka.clients.consumer.ConsumerConfig;

/**
 * Host for the chain parser jobs
 */
public abstract class ParserJob {

    protected static final String PARAM_CHAIN_CONFIG = "chain";
    protected static final String PARAM_CHAIN_CONFIG_FILE = "chain.file";
    protected static final String PARAM_TOPIC_MAP_CONFIG = "chain.topic.map";
    protected static final String PARAM_TOPIC_MAP_CONFIG_FILE = "chain.topic.map.file";
    public static final String PARAM_PRIVATE_KEY_FILE = "key.private.file";
    public static final String PARAM_PRIVATE_KEY = "key.private.base64";
    public static final String PARSER_ERROR_SIDE_OUTPUT = "parser-error";
    public static final String SIGNATURE_ENABLED = "signature.enabled";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        String chainConfig = readConfigMap(PARAM_CHAIN_CONFIG_FILE, PARAM_CHAIN_CONFIG, params, null);
        String topicConfig = readConfigMap(PARAM_TOPIC_MAP_CONFIG_FILE, PARAM_TOPIC_MAP_CONFIG, params, "{}");

        ParserChainMap chainSchema = JSONUtils.INSTANCE.load(chainConfig, ParserChainMap.class);
        TopicPatternToChainMap topicMap = JSONUtils.INSTANCE.load(topicConfig, TopicPatternToChainMap.class);
        String defaultKafkaBootstrap = params.get(Utils.KAFKA_PREFIX + ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);

        DataStream<MessageToParse> source = createSource(env, params, topicMap);

        PrivateKey privateKey = null;
        if (params.getBoolean(SIGNATURE_ENABLED, true)) {
            byte[] privKeyBytes = params.has(PARAM_PRIVATE_KEY_FILE) ?
                    Files.readAllBytes(Paths.get(params.get(PARAM_PRIVATE_KEY_FILE))) :
                    Base64.getDecoder().decode(params.getRequired(PARAM_PRIVATE_KEY));

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privKeyBytes);
            privateKey = keyFactory.generatePrivate(privSpec);
        }

        SingleOutputStreamOperator<Message> results =
                source.process(new ChainParserMapFunction(chainSchema, topicMap, privateKey, defaultKafkaBootstrap))
                        .name("Parser").uid("parser");
        writeResults(params, results);

        writeOriginalsResults(params, source);

        final OutputTag<Message> outputTag = new OutputTag<Message>(PARSER_ERROR_SIDE_OUTPUT) {
        };
        DataStream<Message> parserErrors = results.getSideOutput(outputTag);
        writeErrors(params, parserErrors);

        return env;
    }

    private String readConfigMap(String fileParamKey, String inlineConfigKey, ParameterTool params,
            String defaultConfig) throws IOException {
        if (params.has(fileParamKey)) {
            return new String(Files.readAllBytes(Paths.get(params.getRequired(fileParamKey))), StandardCharsets.UTF_8);
        }
        return defaultConfig == null ? params.getRequired(inlineConfigKey)
                : params.get(inlineConfigKey, defaultConfig);

    }

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);

    protected abstract void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results);

    protected abstract void writeErrors(ParameterTool params, DataStream<Message> errors);

    protected abstract DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params,
            TopicPatternToChainMap topicPatternToChainMap);
}
