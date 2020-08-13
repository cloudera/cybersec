package com.cloudera.cyber.flink;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public interface CyberJob {
    StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception;
}
