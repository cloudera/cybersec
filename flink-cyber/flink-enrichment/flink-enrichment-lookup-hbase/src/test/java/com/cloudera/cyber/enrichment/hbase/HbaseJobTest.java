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

package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;

@Ignore
public class HbaseJobTest extends HbaseJob {
    private transient ManualSource<Message> source;
    private final CollectingSink<EnrichmentCommandResponse> enrichmentResponseSink = new CollectingSink<>();
    private final CollectingSink<Message> sink = new CollectingSink<>();

    @Override
    protected DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        return JobTester.createManualSource(env, TypeInformation.of(EnrichmentCommand.class)).getDataStream();
    }

    @Override
    public DataStream<EnrichmentCommandResponse> writeEnrichments(StreamExecutionEnvironment env, ParameterTool params,
                                                                  DataStream<EnrichmentCommand> enrichmentSource,
                                                                  EnrichmentsConfig enrichmentsConfig) {
        // usually this would send to hbase
        return null;
    }

    @Override
    protected void writeQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> enrichmentResults) {
        enrichmentResults.addSink(enrichmentResponseSink);
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream();
    }

    @Test
    public void test() throws Exception {
        JobTester.startTest(createPipeline(ParameterTool.fromMap(ImmutableMap.of(
                PARAMS_CONFIG_FILE, "config.json"
        ))));
        source.sendRecord(TestUtils.createMessage(Collections.singletonMap("hostname", "test")), 0);
        JobTester.stopTest();


    }


}
