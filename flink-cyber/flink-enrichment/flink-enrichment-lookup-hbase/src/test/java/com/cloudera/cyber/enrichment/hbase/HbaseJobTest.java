package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;

@Ignore
public class HbaseJobTest extends HbaseJob {
    private transient ManualSource<Message> source;
    private transient ManualSource<EnrichmentEntry> enrichmentsSource;
    private transient StreamTableEnvironment tableEnv;
    private CollectingSink<Message> sink = new CollectingSink<Message>();

    @Override
    protected DataStream<EnrichmentEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        enrichmentsSource = JobTester.createManualSource(env, TypeInformation.of(EnrichmentEntry.class));
        return enrichmentsSource.getDataStream();
    }

    @Override
    protected void writeEnrichments(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentEntry> enrichmentSource) {
        // usually this would send to hbase
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream();
    }

    @Test
    public void test() throws Exception {
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAMS_CONFIG_FILE, "config.json"
        ))));
        source.sendRecord(TestUtils.createMessage(Collections.singletonMap("hostname", "test")), 0);

        JobTester.stopTest();
    }
}
