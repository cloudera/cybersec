package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public abstract class SearchIndexJob {


    protected StreamExecutionEnvironment createPipeline(ParameterTool params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);

        // load the relevant index templates from Elastic, and use to build field filters
        Map<String, Set<String>> fieldsBySource = loadFieldsFromIndex(params);

        KeyedStream<Map.Entry<String, Set<String>>, String> entryStringKeyedStream = env.fromCollection(fieldsBySource.entrySet()).keyBy(Map.Entry::getKey);

        // TODO - apply any filtering and index projection logic here
        KeyedStream<Message, String> messages = createSource(env, params).keyBy(Message::getSource);
        messages.connect(entryStringKeyedStream.keyBy(e -> e.getKey())).process(new FilterStreamFieldsByConfig())
                .name("Index Entry Extractor").uid("index-entry-extract");

        // now add the correctly formed version for tables

        // this will read an expected schema and map the incoming messages by type to the expected output fields
        // unknown fields will either be dumped in a 'custom' map field or dropped based on config

        writeResults(source, params);

        return env;
    }

    protected abstract Map<String, Set<String>> loadFieldsFromIndex(ParameterTool params) throws IOException;
    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
    protected abstract void writeResults(DataStream<Message> results, ParameterTool params) throws IOException;
}
