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

package com.cloudera.cyber.enrichment.cidr;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.Message.MessageBuilder;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.cidr.impl.IpRegionCidrEnrichment;
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
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

@Log
public class IpRegionJobTest extends IpRegionCidrJob {
    private static final String STRING_IP_FIELD_NAME = "string_ip_field";
    private static final String LIST_IP_FIELD_NAME = "list_ip_field";
    private ManualSource<Message> source;
    private final CollectingSink<Message> sink = new CollectingSink<>();
    private final List<Message> recordLog = new ArrayList<>();
    public static final String CIDR_CONFIG_PATH = "./src/test/resources/cidr.json";
    public static final String UN_EXIST_CIDR_CONFIG_PATH = "./src/test/resources/unExist.json";
    public static final String EMPTY_CIDR_CONFIG_PATH = "./src/test/resources/empty-cidr.json";


    @Test
    public void testIpGeoPipeline() throws Exception {
        long ts = 0;

        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAM_CIDR_IP_FIELDS, String.join(",", STRING_IP_FIELD_NAME, LIST_IP_FIELD_NAME),
                PARAM_CIDR_CONFIG_PATH, CIDR_CONFIG_PATH
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
    }
    @Test
    public void testIpGeoPipelineWithEmptyIpFields() throws Exception {
        long ts = 0;

        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAM_CIDR_IP_FIELDS, "",
                PARAM_CIDR_CONFIG_PATH, CIDR_CONFIG_PATH
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
        assertThat(output.stream().flatMap(m -> m.getExtensions().entrySet().stream()).collect(Collectors.toList()))
                .contains(
                        entry(STRING_IP_FIELD_NAME, IpRegionCidrTestData.IPV4_10_ADDRESS),
                        entry(LIST_IP_FIELD_NAME, IpRegionCidrTestData.IPV6_15_ADDRESS));

    }

    @Test
    public void testIpGeoPipelineWithNotExistConfigFile() throws Exception {
        assertThatThrownBy(() -> JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAM_CIDR_IP_FIELDS, "",
                PARAM_CIDR_CONFIG_PATH, UN_EXIST_CIDR_CONFIG_PATH
        ))).setParallelism(1))).isInstanceOf(IOException.class)
                .hasMessageContaining("No such file or directory")
                .hasMessageNotContaining("File is empty");
    }

    @Test
    public void testIpGeoPipelineWithEmptyConfigFile() throws Exception {
        assertThatThrownBy(() -> JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAM_CIDR_IP_FIELDS, "",
                PARAM_CIDR_CONFIG_PATH, EMPTY_CIDR_CONFIG_PATH
        ))).setParallelism(1))).isInstanceOf(IOException.class)
                .hasMessageContaining("File is empty")
                .hasMessageContaining("src/test/resources/empty-cidr.json")
                .hasMessageNotContaining("No such file or directory");
    }

    private void createMessages(long ts) {
        long offset = 100;
        List<SimpleEntry<String, String>> entries = Arrays.asList(
                new SimpleEntry<>(STRING_IP_FIELD_NAME, IpRegionCidrTestData.IPV4_10_ADDRESS),
                new SimpleEntry<>(LIST_IP_FIELD_NAME, IpRegionCidrTestData.IPV6_15_ADDRESS)
        );
        for (SimpleEntry<String, String> entry : entries) {
            MessageBuilder builder = TestUtils.createMessage().toBuilder()
                    .extensions(ImmutableMap.of(entry.getKey(), entry.getValue()))
                    .ts(ts + offset);
            sendRecord(builder);
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
        assertThat(results.stream().flatMap(m -> m.getExtensions().entrySet().stream()).collect(Collectors.toList()))
                .contains(
                        entry(STRING_IP_FIELD_NAME, IpRegionCidrTestData.IPV4_10_ADDRESS),
                        entry(STRING_IP_FIELD_NAME + Enrichment.DELIMITER + IpRegionCidrEnrichment.FEATURE_NAME, IpRegionCidrTestData.IPV4_MASK_REGION_1_NAME),
                        entry(LIST_IP_FIELD_NAME, IpRegionCidrTestData.IPV6_15_ADDRESS),
                        entry(LIST_IP_FIELD_NAME + Enrichment.DELIMITER + IpRegionCidrEnrichment.FEATURE_NAME, IpRegionCidrTestData.IPV6_MASK_REGION_2_NAME)
                );
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }

    @Override
    protected SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> ipFields) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream().map(s -> s);
    }

    @After
    public void clean() throws Exception{
        JobTester.stopTest();
    }
}



