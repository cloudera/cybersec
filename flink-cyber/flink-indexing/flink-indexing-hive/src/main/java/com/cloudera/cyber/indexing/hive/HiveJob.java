package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.scoring.ScoredMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

@Slf4j
public abstract class HiveJob {

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<ScoredMessage> source = createSource(env, params);
        source.addSink(new StreamingHiveSink(params)).name("Hive Streaming Indexer");

        return env;
    }

    protected abstract SingleOutputStreamOperator<ScoredMessage> createSource(StreamExecutionEnvironment env, ParameterTool params);
}
