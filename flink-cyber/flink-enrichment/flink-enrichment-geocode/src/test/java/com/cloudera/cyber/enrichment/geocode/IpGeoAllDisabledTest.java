package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IpGeoAllDisabledTest extends IpGeoTestBase {
    @Test
    public void testAllGeoPipeline() throws Exception {
        long ts = 0;
        try (StreamExecutionEnvironment env = createPipeline(ParameterTool.fromMap(
            ImmutableMap.of(PARAMS_ENABLE_ASN, "false",
                PARAMS_ENABLE_GEO, "false",
                PARAMS_ENABLE_COMPANY, "false"))).setParallelism(1)) {
            JobTester.startTest(env);

            createMessages(ts);

            JobTester.stopTest();

            checkResults();
        }
    }

    private void createMessages(long ts) {

        List<Message.MessageBuilder> messages = new ArrayList<>();

        messages.add(TestUtils.createMessage().toBuilder()
                .extensions(new HashMap<>() {{
                    put(STRING_IP_FIELD_NAME, IpAsnTestData.IP_WITH_NUMBER_AND_ORG);
                }}));
        messages.add(TestUtils.createMessage().toBuilder()
                .extensions(new HashMap<>() {{
                    put(STRING_IP_FIELD_NAME, IpAsnTestData.IP_V6_WITH_NUMBER_AND_ORG);
                }}));
        messages.add(TestUtils.createMessage().toBuilder()
                .extensions(new HashMap<>() {{
                    put(STRING_IP_FIELD_NAME, IpAsnTestData.IP_NOT_FOUND);
                }}));

        long offset = 100;
        for(Message.MessageBuilder nextBuilder: messages) {
            Map<String, String> inputFields = nextBuilder.build().getExtensions();

            long nextTimestamp = ts + offset;
            // all enrichments are disabled, no enrichments are added
            expectedExtensions.put(nextTimestamp, inputFields);
            sendRecord(nextBuilder.ts(nextTimestamp));
            offset += 100;
        }

        source.sendWatermark(ts + 1000);
        source.markFinished();
    }

}
