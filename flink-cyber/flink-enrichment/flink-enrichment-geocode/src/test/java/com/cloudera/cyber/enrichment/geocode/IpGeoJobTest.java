package com.cloudera.cyber.enrichment.geocode;

import akka.io.Tcp;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.enrichment.geocode.impl.IpGeoEnrichment;
import lombok.extern.java.Log;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

@Log
@Ignore
public class IpGeoJobTest extends IpGeoJob {
    private static final String STRING_IP_FIELD_NAME = "string_ip_field";
    private static final String LIST_IP_FIELD_NAME = "list_ip_field";
    private ManualSource<Message> source;
    private final CollectingSink<Message> sink = new CollectingSink<>();
    private final List<Message> recordLog = new ArrayList<>();
    private final Map<Long, Map<String, String>> expectedExtensions = new HashMap<>();

    @Test
    public void testIpGeoPipeline() throws Exception {
        long ts = 0;

        JobTester.startTest(createPipeline(ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAM_GEO_FIELDS, String.join(",", STRING_IP_FIELD_NAME, LIST_IP_FIELD_NAME));
            put(PARAM_GEO_DATABASE_PATH, IpGeoTestData.GEOCODE_DATABASE_PATH);
        }})).setParallelism(1));

        createMessages(ts);

        JobTester.stopTest();

        List<Message> output = new ArrayList<>();

        int recordCount = recordLog.size();
        for (int i = 0; i < recordCount; i++) {
            try {
                output.add(sink.poll(Duration.ofMillis(100)));
            } catch (TimeoutException e) {
                log.info("Caught timeout exception.");
            }
        }
        checkResults(output);
        assertThat("Result count", output, hasSize(recordLog.size()));
    }

    private void createMessages(long ts) {

        List<String> ipList = Arrays.asList(IpGeoTestData.LOCAL_IP,IpGeoTestData.ALL_FIELDS_IPv4, IpGeoTestData.ALL_FIELDS_IPv4, IpGeoTestData.UNKNOWN_HOST_IP, IpGeoTestData.COUNTRY_ONLY_IPv6);
        List<Message.MessageBuilder> messages = new ArrayList<>();

        messages.add(TestUtils.createMessage().toBuilder()
                .extensions(new HashMap<String, String>() {{
                    put(STRING_IP_FIELD_NAME, IpGeoTestData.COUNTRY_ONLY_IPv6);
                }}));
        messages.add(TestUtils.createMessage().toBuilder()
                .extensions(new HashMap<String, String>() {{
                    put(STRING_IP_FIELD_NAME, IpGeoTestData.ALL_FIELDS_IPv4);
                }}));

        long offset = 100;
        for(Message.MessageBuilder nextBuilder: messages) {
            Map<String, String> inputFields = nextBuilder.build().getExtensions();
            Map<String, String> expectedExtension = new HashMap<>(inputFields);
            long nextTimestamp = ts + offset;
            expectedExtensions.put(nextTimestamp, expectedExtension);
            inputFields.forEach((field, value) -> IpGeoTestData.getExpectedEnrichmentValues(expectedExtension, field, value));
            sendRecord(nextBuilder.ts(ts + offset));
            offset += 100;
        }

        source.sendWatermark(ts + 1000);
        source.markFinished();
    }

    private void sendRecord(Message.MessageBuilder d) {
        Message r = d.id(UUID.randomUUID().toString())
                .originalSource(TestUtils.source("test", 0, 0))
                .build();
        this.source.sendRecord(r, r.getTs());
        this.recordLog.add(r);
    }

    private void checkResults(List<Message> results) {

        for(Message result : results) {
            assertThat(String.format("extension fields match - index %d", result.getTs()), result.getExtensions(), Matchers.equalTo(expectedExtensions.get(result.getTs())));
        }
        assertThat("expected number of results returned", expectedExtensions.size(), Matchers.equalTo(results.size()));

    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> ipFields) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream();
    }
}



