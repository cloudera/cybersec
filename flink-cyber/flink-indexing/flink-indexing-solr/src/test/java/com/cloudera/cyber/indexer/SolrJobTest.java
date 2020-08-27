package com.cloudera.cyber.indexer;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.indexing.IndexEntry;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;

public class SolrJobTest extends SolrJob {
    private ManualSource<Message> source;

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream();
    }

    @Override
    protected void writeResults(DataStream<IndexEntry> results, ParameterTool params) {

    }
}
