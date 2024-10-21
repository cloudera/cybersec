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

package com.cloudera.cyber.test;

import com.cloudera.cyber.test.generator.CaracalGeneratorFlinkJob;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class TestCaracalGeneratorJob extends CaracalGeneratorFlinkJob {
    private final CollectingSink<Tuple2<String, byte[]>> sink = new CollectingSink<>();
    private final CollectingSink<Tuple2<String, Integer>> metricSink = new CollectingSink<>();

    @Test
    public void testGenerator() throws Exception {
        int count = 100;
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
            PARAMS_RECORDS_LIMIT, String.valueOf(count)
        ))));

        JobTester.stopTest();

        checkGeneratedResults(count, Arrays.asList(THREAT_TOPIC_NAME, NETFLOW_TOPIC, NETFLOW_B_TOPIC, DPI_HTTP_TOPIC, DPI_DNS_TOPIC, DPI_SMTP_TOPIC));
    }

    @Test
    public void testAvroGenerator() throws Exception {
        int count = 100;
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
            PARAMS_RECORDS_LIMIT, String.valueOf(count),
            PARAMS_SCHEMA, Boolean.toString(true)
        ))));

        JobTester.stopTest();

       checkGeneratedResults(count, Collections.singletonList(GENERATOR_AVRO_TOPIC));
    }

    @Test
    public void testCustomGenerator() throws Exception {
        int count = 100;
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAMS_RECORDS_LIMIT, String.valueOf(count),
                PARAMS_GENERATOR_CONFIG, Objects.requireNonNull(getClass().getClassLoader().getResource("config/generator_config_test.json")).toExternalForm()
        ))));

        JobTester.stopTest();

        checkGeneratedResults(count, Lists.newArrayList("squid", "squid_scenario"));
    }

    @Test
    public void testRelativeCustomGenerator() throws Exception {
        int count = 100;
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAMS_RECORDS_LIMIT, String.valueOf(count),
                PARAMS_GENERATOR_CONFIG, Objects.requireNonNull(getClass().getClassLoader().getResource("config/relative_generator_config.json")).toExternalForm()
        ))));

        JobTester.stopTest();

        checkGeneratedResults(count, Lists.newArrayList(THREAT_TOPIC_NAME, "squid", "squid_scenario"));
    }

    @Test
    public void testAvroWithCustomError() {
        assertThatThrownBy( () ->
            JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                    PARAMS_RECORDS_LIMIT, String.valueOf(100),
                    PARAMS_GENERATOR_CONFIG, Objects.requireNonNull(getClass().getClassLoader().getResource("config/relative_generator_config.json")).toExternalForm(),
                    PARAMS_SCHEMA, Boolean.toString(true)
            ))))).isInstanceOf(IllegalStateException.class).hasMessage(AVRO_WITH_CUSTOM_CONFIG_ERROR);
   }

    private void checkGeneratedResults(int expectedCount, List<String> expectedTopics) throws TimeoutException {
        List<Tuple2<String, byte[]>> results = new ArrayList<>();
        for (int i = 0; i < expectedCount; i++) {
            Tuple2<String, byte[]> generatedRecord = sink.poll(Duration.ofMillis(100));
            String actualTopic = generatedRecord.f0;
            Assert.assertTrue(String.format("Generated topic '%s' is not in expected topics '%s'", actualTopic, expectedTopics), expectedTopics.contains(actualTopic));
            results.add(generatedRecord);
        }

        assertThat("Has generated results", results, hasSize(expectedCount));
    }

    @Override
    protected void writeMetrics(ParameterTool params, SingleOutputStreamOperator<Tuple2<String, Integer>> metrics) {
        metrics.addSink(metricSink);
    }

    @Override
    protected void writeResults(ParameterTool params, SingleOutputStreamOperator<Tuple2<String, byte[]>> generatedInput) {
        generatedInput.addSink(sink);
    }

}
