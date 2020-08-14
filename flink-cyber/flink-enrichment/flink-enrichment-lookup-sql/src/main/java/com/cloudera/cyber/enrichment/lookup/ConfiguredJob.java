package com.cloudera.cyber.enrichment.lookup;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public interface ConfiguredJob<T> {
    DataStream<ConfigurationCommand<T>> createConfigStream(StreamExecutionEnvironment env, ParameterTool params);
}
