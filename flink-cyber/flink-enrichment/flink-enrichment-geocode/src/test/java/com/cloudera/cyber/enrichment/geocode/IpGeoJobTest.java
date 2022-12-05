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
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
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
    private final Map<Long, Map<String, String>> expectedExtensions = new HashMap<>();

    @Test
    public void testIpGeoPipeline() throws Exception {
        long ts = 0;

        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
            PARAM_GEO_FIELDS, String.join(",", STRING_IP_FIELD_NAME, LIST_IP_FIELD_NAME),
            PARAM_GEO_DATABASE_PATH, IpGeoTestData.GEOCODE_DATABASE_PATH
        ))).setParallelism(1));

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
    protected SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> ipFields) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream().map(s->s);
    }
}



