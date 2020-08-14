package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.sha1;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

public class LookupJobTest extends LookupJob {

    CollectingSink<Message> sink = new CollectingSink<>();
    private ManualSource<Message> source;
    private ManualSource<EnrichmentEntry> enrichmentSource;

    @Test
    public void testEnrichments() throws Exception {
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAMS_CONFIG_FILE, "config.json"
        ))));

        // make up some enrichments (three types, multiple fields and multiple entries in some)

        sendEnrichment("ip_whitelist", "10.0.0.1", Collections.singletonMap("whitelist", "true"));
        sendEnrichment("ip_whitelist", "192.168.0.1", Collections.singletonMap("whitelist", "false"));
        sendEnrichment("internal_ip", "10.0.0.1", new HashMap<String, String>(2) {{
            put("field1", "1");
            put("field2", "2");
        }});
        sendEnrichment("asset", "10.0.0.1", new HashMap<String, String>(2) {{
            put("owner", "mew");
            put("location", "office");
        }});

        // make up some enrichable messages
        sendMessage("one enrichment", Collections.singletonMap("ip_src_addr", "10.0.0.1"), 100);
        sendMessage("enrichment on two fields", new HashMap<String, Object>(2) {{
            put("ip_src_addr", "10.0.0.1");
            put("ip_dst_addr", "192.168.0.1");
        }}, 150);
        sendMessage("no enrichment hit", Collections.singletonMap("ip_src_addr", "10.0.0.2"), 200);

        JobTester.stopTest();

        // assert that the sink contains fully enriched entities
        boolean running = true;
        List<Message> results = new ArrayList<>();
        while (running) {
            try {
                Message message = sink.poll(Duration.ofMillis(100));
                assertThat("message is not null", message, notNullValue());
                results.add(message);
            } catch (TimeoutException e) {
                running = false;
            }
        }
        assertThat("All message received", results, hasSize(3));

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

    private EnrichmentEntry enrichment(String type, String key, Map<String, String> entries) {
        return EnrichmentEntry.newBuilder()
                .setType(type)
                .setKey(key)
                .setEntries(entries)
                .setTs(0)
                .build();
    }

    private void sendEnrichment(String type, String key, Map<String, String> entries) {
        enrichmentSource.sendRecord(enrichment(type, key, entries), 0);
    }

    private Message message(String message, Map<String, Object> extensions, long ts) {
        return Message.newBuilder()
                .setOriginalSource(SignedSourceKey.newBuilder()
                        .setTopic("test")
                        .setPartition(0)
                        .setOffset(0)
                        .setSignature(new sha1(new byte[128])).build())
                .setId(UUID.randomUUID().toString())
                .setTs(ts)
                .setMessage(message)
                .setSource("test")
                .setExtensions(extensions)
                .build();
    }

    private void sendMessage(String message, Map<String, Object> extensions, long ts) {
        source.sendRecord(message(message, extensions, ts), ts);
    }
}
