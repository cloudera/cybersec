package com.cloudera.cyber.indexer;

import com.cloudera.cyber.Message;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public abstract class ParquetJob {

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<Message> source = createSource(env, params);

        writeResults(source, params);

        return env;
    }
    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
    protected abstract void writeResults(DataStream<Message> results, ParameterTool params);
}
