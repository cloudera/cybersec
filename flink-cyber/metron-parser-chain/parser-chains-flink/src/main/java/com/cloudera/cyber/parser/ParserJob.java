package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.parserchains.core.utils.JSONUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Host for the chain parser jobs
 */
public abstract class ParserJob {

    protected static final String PARAM_CHAIN_CONFIG = "chain";
    public static final String PARAM_PRIVATE_KEY_FILE = "key.private.file";
    public static final String PARAM_PRIVATE_KEY = "key.private.base64";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        String chainConfig = params.getRequired(PARAM_CHAIN_CONFIG);

        ParserChainMap chainSchema = JSONUtils.INSTANCE.load(chainConfig, ParserChainMap.class);

        Map<String, String> topicMap = new HashMap<>();

        DataStream<MessageToParse> source = createSource(env, params);


        byte[] privKeyBytes = params.has(PARAM_PRIVATE_KEY_FILE) ?
                Files.readAllBytes(Paths.get(params.get(PARAM_PRIVATE_KEY_FILE))) :
                Base64.getDecoder().decode(params.getRequired(PARAM_PRIVATE_KEY));

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privKeyBytes);
        PrivateKey privateKey = keyFactory.generatePrivate(privSpec);

        SingleOutputStreamOperator<Message> results = source.flatMap(new ChainParserMapFunction(chainSchema, topicMap, privateKey));
        writeResults(params, results);

        return env;
    }

    private void printResults(SingleOutputStreamOperator<Message> results) {
        results.print();
    }

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);

    protected abstract void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results);

    protected abstract DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params);
}
