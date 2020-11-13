package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.KeyedStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;

import java.io.IOException;

public class SolrJobTest extends SolrJob {
    private ManualSource<Message> source;
    private CollectingSink<IndexEntry> output = new CollectingSink<>();
    private CollectingSink<MessageRetry> retrySink = new CollectingSink<>();

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream();
    }

    @Override
    protected KeyedStream<CollectionField, String> createConfigSource(StreamExecutionEnvironment env, ParameterTool params) {
        return null;
    }


    @Override
    protected void writeResults(DataStream<IndexEntry> results, ParameterTool params) {
        results.addSink(output);
    }

    @Override
    protected void logConfig(DataStream<CollectionField> configSource, ParameterTool params) {
    }

}
