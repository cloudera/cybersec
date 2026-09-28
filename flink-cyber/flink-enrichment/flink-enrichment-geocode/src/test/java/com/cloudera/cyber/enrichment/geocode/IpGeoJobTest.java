/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.google.common.collect.ImmutableMap;
import lombok.extern.java.Log;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log
public class IpGeoJobTest extends IpGeoTestBase {

    @Test
    public void testIpGeoPipeline() throws Exception {
        long ts = 0;
        try (StreamExecutionEnvironment env = createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAM_GEO_FIELDS, String.join(",", STRING_IP_FIELD_NAME, IpGeoTestData.LIST_IP_FIELD_NAME),
                PARAM_GEO_DATABASE_PATH, IpGeoTestData.GEOCODE_DATABASE_PATH,
                PARAMS_ENABLE_ASN, "false",
                PARAMS_ENABLE_COMPANY, "false"
        ))).setParallelism(1)) {
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
                    put(STRING_IP_FIELD_NAME, IpGeoTestData.COUNTRY_ONLY_IPv6);
                }}));
        messages.add(TestUtils.createMessage().toBuilder()
                .extensions(new HashMap<>() {{
                    put(STRING_IP_FIELD_NAME, IpGeoTestData.ALL_FIELDS_IPv4);
                }}));

        long offset = 100;
        for(Message.MessageBuilder nextBuilder: messages) {
            Map<String, String> inputFields = nextBuilder.build().getExtensions();
            long nextTimestamp = ts + offset;
            expectedExtensions.put(nextTimestamp, IpGeoTestData.getExpectedExtension(inputFields, List.of(STRING_IP_FIELD_NAME, IpGeoTestData.LIST_IP_FIELD_NAME)));
            sendRecord(nextBuilder.ts(ts + offset));
            offset += 100;
        }

        source.sendWatermark(ts + 1000);
        source.markFinished();
    }
}



