package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public abstract class ParquetJob {

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);

        // TODO - apply any filtering and index projection logic here

        // now add the correctly formed version for tables

        // this will read an expected schema and map the incoming messages by type to the expected output fields
        // unknown fields will either be dumped in a 'custom' map field or dropped based on config


        writeResults(source, params);

        return env;
    }
    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
    protected abstract void writeResults(DataStream<Message> results, ParameterTool params);
    protected abstract void writeTabularResults(DataStream<Message> results, String path, ParameterTool params);
}
