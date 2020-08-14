package com.cloudera.cyber.enrichment.lookup;

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
import org.hamcrest.collection.IsCollectionWithSize;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.KafkaContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;

public class LookupJobTest<sink> extends LookupJob {

    private transient ManualSource<Message> messagesSource;
    private transient ManualSource<EnrichmentEntry> enrichmentsSource;
    private transient StreamTableEnvironment tableEnv;
    private CollectingSink<Message> sink = new CollectingSink<Message>();

    @Rule
    public KafkaContainer kafka = new KafkaContainer();


    @Test
    public void testLookup() throws Exception {
        ParameterTool params = ParameterTool.fromMap(new HashMap<String, String>() {{
            put("kafka.bootstrap.servers",kafka.getBootstrapServers());
        }});
        StreamExecutionEnvironment env = createPipeline(params);

        JobTester.startTest(env);

        // send some enrichments
        enrichmentsSource.sendRecord(EnrichmentEntry.newBuilder()
                .setKey("10.0.0.1")
                .setEntries(singletonMap("whitelist", "true"))
                .setType("ip_whitelist")
                .setTs(10l)
                .build(), 10);

        enrichmentsSource.sendWatermark(1000);

        // send some messages
        messagesSource.sendRecord(Message.newBuilder()
                .setOriginalSource(TestUtils.source("test", 0,0))
                .setId(UUID.randomUUID().toString())
                .setExtensions(new HashMap<String, Object>(){{
                    put("ip_src_addr", "10.0.0.1");
                    put("ip_dst_addr", "10.0.0.1");
                }})
                .setTs(1000l)
                .setMessage("Should be enriched")
                .setSource("test")
                .build());


        messagesSource.sendRecord(Message.newBuilder()
                .setOriginalSource(TestUtils.source("test", 0,0))
                .setId(UUID.randomUUID().toString())
                .setExtensions(new HashMap<String, Object>(){{
                        put("ip_src_addr", "192.168.0.1");
                        put("ip_dst_addr", "10.0.0.1");
                }})
                .setTs(1000l)
                .setMessage("Should not be enriched")
                .setSource("test")
                .build());

        messagesSource.sendWatermark(1000);


        enrichmentsSource.sendRecord(EnrichmentEntry.newBuilder()
                .setKey("10.0.0.1")
                .setEntries(singletonMap("later", "after 1500"))
                .setType("ip_whitelist")
                .setTs(1500l)
                .build(), 1500);

        enrichmentsSource.sendWatermark(2000);

        messagesSource.sendRecord(Message.newBuilder()
                .setOriginalSource(TestUtils.source("test", 0,0))
                .setId(UUID.randomUUID().toString())
                .setExtensions(singletonMap("ip_src_addr", "10.0.0.1"))
                .setTs(2000l)
                .setMessage("Should be enriched with later enrichment")
                .setSource("test")
                .build());

        messagesSource.sendWatermark(2000);
        // query the output table

        JobTester.stopTest();

        final ArrayList<Message> results = new ArrayList<Message>();

        results.add(sink.poll(Duration.ofMillis(100)));
        results.add(sink.poll(Duration.ofMillis(100)));

        assertThat("Results found", results, IsCollectionWithSize.hasSize(2));

        // determine the state of the lookup table is correct




    }


    @Override
    protected DataStream<EnrichmentEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        enrichmentsSource = JobTester.createManualSource(env, TypeInformation.of(EnrichmentEntry.class));
        return enrichmentsSource.getDataStream();
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        messagesSource = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return messagesSource.getDataStream();
    }
}