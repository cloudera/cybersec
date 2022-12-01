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

package com.cloudera.cyber.indexing.elastic;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.indexing.CollectionField;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.*;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import static org.junit.Assert.fail;

public class ElasticJobTest extends ElasticJob {

    private ManualSource<Message> source;
    private ManualSource<CollectionField> configSource;

    @Rule
    public ElasticsearchContainer container = new ElasticsearchContainer();

    @Before
    public void before() {
        container.start();
    }

    @After
    public void after() {
        container.stop();
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream();
    }

    @Override
    protected DataStream<CollectionField> createConfigSource(StreamExecutionEnvironment env, ParameterTool params) {
        configSource = JobTester.createManualSource(env, TypeInformation.of(CollectionField.class));
        return configSource.getDataStream();
    }

    @Override
    protected void logConfig(DataStream<CollectionField> configSource, ParameterTool params) {
    }

    @Test
    @Ignore
    public void testSingleDestination() {
        fail("Not implemented");
    }

    @Test
    @Ignore
    public void testMultipleDestination() {
        fail("Not implemented");
    }

    @Test
    @Ignore
    public void testFieldFiltering() {
        fail("Not implemented");
    }

    @Test
    @Ignore
    public void testEventFiltering() {
        fail("Not implemented");
    }
}
