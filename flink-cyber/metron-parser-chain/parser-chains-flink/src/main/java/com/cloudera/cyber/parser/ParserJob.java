package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.parserchains.core.utils.JSONUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.HashMap;
import java.util.Map;

/**
 * Host for the chain parser jobs
 */
public abstract class ParserJob {

    protected static final String PARAM_CHAIN_CONFIG = "chain";

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        String chainConfig = params.getRequired(PARAM_CHAIN_CONFIG);

        ParserChainMap chainSchema = JSONUtils.INSTANCE.load(chainConfig, ParserChainMap.class);

        Map<String,String> topicMap = new HashMap<>();

        DataStream<MessageToParse> source = createSource(env, params);

        SingleOutputStreamOperator<Message> results = source.flatMap(new ChainParserMapFunction(chainSchema, topicMap));
        writeResults(params, results);

        return env;
    }

    private void printResults(SingleOutputStreamOperator<Message> results) {
        results.print();
    }

    protected abstract void writeResults(ParameterTool params, DataStream<Message> results);
    protected abstract DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params);
}
