package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import lombok.extern.java.Log;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

@Log
public class IpGeoJobTest extends IpGeoJob {
    private static final String STRING_IP_FIELD_NAME = "string_ip_field";
    private static final String LIST_IP_FIELD_NAME = "list_ip_field";
    private ManualSource<Message> source;
    private final CollectingSink<Message> sink = new CollectingSink<>();
    private final List<Message> recordLog = new ArrayList<>();
    private final List<Map<String, Object>> expectedExtensions = new ArrayList<>();

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

        for (int i = 0; i < 10; i++) {
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
        List<Message.Builder>  messages = new ArrayList<>();
        messages.add(Message.newBuilder()
                .setExtensions(new HashMap<String, Object>() {{
                    put(STRING_IP_FIELD_NAME, IpGeoTestData.COUNTRY_ONLY_IPv6);
                }}));
         messages.add(Message.newBuilder()
                .setExtensions(new HashMap<String, Object>() {{
                    put(STRING_IP_FIELD_NAME, IpGeoTestData.ALL_FIELDS_IPv4);
                }}));
        messages.add(Message.newBuilder()
                .setExtensions(new HashMap<String, Object>() {{
                    put(LIST_IP_FIELD_NAME, ipList);
                }}));
        messages.add(Message.newBuilder()
                .setExtensions(new HashMap<String, Object>() {{
                    put(STRING_IP_FIELD_NAME, IpGeoTestData.ALL_FIELDS_IPv4);
                    put(LIST_IP_FIELD_NAME, ipList);
                }}));

        long offset = 100;
        for(Message.Builder nextBuilder: messages) {
            Map<String, Object> inputFields = nextBuilder.getExtensions();
            Map<String, Object> expectedExtension = new HashMap<>(inputFields);
            expectedExtensions.add(expectedExtension);
            inputFields.forEach((field, value) -> IpGeoTestData.getExpectedEnrichmentValues(expectedExtension, field, value));
            sendRecord(nextBuilder.setTs(ts + offset));
            offset += 100;
        }

        source.sendWatermark(ts + 1000);
        source.markFinished();
    }

    private void sendRecord(Message.Builder d) {
        Message r = d.setId(UUID.randomUUID().toString())
                .setOriginalSource(TestUtils.source("test", 0, 0))
                .build();
        this.source.sendRecord(r, r.getTs());
        this.recordLog.add(r);
    }

    private void checkResults(List<Message> results) {
        int index = 0;
        for(Message result : results) {
            assertThat(String.format("extension fields match - index %d", index), result.getExtensions(), Matchers.equalTo(expectedExtensions.get(index)));
            index += 1;
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



