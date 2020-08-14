package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Test;

import java.util.Collections;

public class LookupJobTest extends LookupJob {

    CollectingSink sink = new CollectingSink<Message>();
    private ManualSource<Message> source;
    private ManualSource<EnrichmentEntry> enrichmentSource;

    @Test
    public void testEnrichments() throws Exception {
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAMS_CONFIG_FILE, "config.json"
        ))));

        // make up some enrichments (three types, multiple fields and multiple entries in some)

        EnrichmentEntry.newBuilder()
                .setType("ip_whitelist")
                .setKey("10.0.0.1")
                .setEntries(Collections.singletonMap("whitelist", "true"))
                .setTs(0).build();
        EnrichmentEntry.newBuilder()
                .setType("ip_whitelist")
                .setKey("192.168.0.1")
                .setEntries(Collections.singletonMap("whitelist", "false"))
                .setTs(0).build();
        
        EnrichmentEntry.newBuilder()
                .setType("internal_ip")
                .setKey("10.0.0.1")
                .setEntries(ImmutableMap.of("field1", "1", "field2", "2"))
                .setTs(0).build();



        // make up some enrichable messages


        JobTester.stopTest();

        // assert that the sink contains fully enriched entities


    }


    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction) {
        reduction.addSink(sink);
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream();
    }

    @Override
    protected DataStream<EnrichmentEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        enrichmentSource = JobTester.createManualSource(env, TypeInformation.of(EnrichmentEntry.class));
        return enrichmentSource.getDataStream();
    }
}
