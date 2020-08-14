package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.Message;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public interface MessageProcessingJob {
    DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
}
