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
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class TestCaracalGeneratorJob extends CaracalGeneratorFlinkJob {
    private final CollectingSink<Tuple2<String, String>> sink = new CollectingSink<>();
    private final CollectingSink<Tuple2<String, Integer>> metricSink = new CollectingSink<>();
    private final CollectingSink<Tuple2<String, byte[]>> binarySink = new CollectingSink<>();

    @Test
    public void testGenerator() throws Exception {
        int count = 100;
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
            PARAMS_RECORDS_LIMIT, String.valueOf(count)
        ))));

        JobTester.stopTest();

        List<Tuple2<String, String>> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(sink.poll(Duration.ofMillis(100)));
        }

        assertThat("Has generated results", results, hasSize(count));
    }

    @Test
    public void testAvroGenerator() throws Exception {
        int count = 100;
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
            PARAMS_RECORDS_LIMIT, String.valueOf(count),
            PARAMS_SCHEMA, Boolean.toString(true)
        ))));

        JobTester.stopTest();

        List<Tuple2<String, byte[]>> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(binarySink.poll(Duration.ofMillis(100)));
        }

        assertThat("Has generated results", results, hasSize(count));
    }

    @Override
    protected void writeMetrics(ParameterTool params, SingleOutputStreamOperator<Tuple2<String, Integer>> metrics) {
        metrics.addSink(metricSink);
    }

    @Override
    protected void writeResults(ParameterTool params, SingleOutputStreamOperator<Tuple2<String, String>> generatedInput) {
        generatedInput.addSink(sink);
    }

    @Override
    protected void writeBinaryResults(ParameterTool params, SingleOutputStreamOperator<Tuple2<String, byte[]>> generatedInput) {
        generatedInput.addSink(binarySink);
    }

}
