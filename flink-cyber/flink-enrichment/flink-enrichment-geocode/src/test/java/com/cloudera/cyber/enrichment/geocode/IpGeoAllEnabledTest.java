package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IpGeoAllEnabledTest extends IpGeoTestBase {

    @Test
    public void testAllGeoPipeline() throws Exception {
        long ts = 0;

        Map<String, String> jobConfigSettings = new HashMap<>();
        jobConfigSettings.put(PARAM_ASN_FIELDS, STRING_IP_FIELD_NAME);
        jobConfigSettings.put(PARAM_ASN_DATABASE_PATH, IpAsnTestData.ASN_DATABASE_PATH);
        jobConfigSettings.put(PARAM_GEO_FIELDS, String.join(",", STRING_IP_FIELD_NAME, IpGeoTestData.LIST_IP_FIELD_NAME));
        jobConfigSettings.put(PARAM_GEO_DATABASE_PATH, IpGeoTestData.GEOCODE_DATABASE_PATH);
        jobConfigSettings.put(PARAM_COMPANY_FIELDS, STRING_IP_FIELD_NAME);
        jobConfigSettings.put(PARAM_COMPANY_DATABASE_PATH, IpCompanyTestData.COMPANY_DATABASE_PATH);
        try (StreamExecutionEnvironment env = createPipeline(ParameterTool.fromMap(jobConfigSettings)).setParallelism(1)) {
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
            Map<String, String> extensions = IpAsnTestData.getExpectedExtension(inputFields, Collections.singletonList(STRING_IP_FIELD_NAME));
            extensions.putAll(IpCompanyTestData.getExpectedExtension(inputFields, Collections.singletonList(STRING_IP_FIELD_NAME)));
            extensions.putAll(IpGeoTestData.getExpectedExtension(inputFields, List.of(STRING_IP_FIELD_NAME, IpGeoTestData.LIST_IP_FIELD_NAME)));

            long nextTimestamp = ts + offset;
            expectedExtensions.put(nextTimestamp, extensions);
            sendRecord(nextBuilder.ts(nextTimestamp));
            offset += 100;
        }

        source.sendWatermark(ts + 1000);
        source.markFinished();
    }

}
