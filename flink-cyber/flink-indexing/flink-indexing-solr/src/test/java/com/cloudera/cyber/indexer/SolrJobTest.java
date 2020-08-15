package com.cloudera.cyber.indexer;

import com.cloudera.cyber.Message;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class SolrJobTest extends SolrJob {
    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return null;
    }

    @Override
    protected void writeResults(DataStream<Message> results, ParameterTool params) {

    }
}
