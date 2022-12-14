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

package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.enrichment.stix.parsing.ThreatIntelligenceDetails;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;

import static com.cloudera.cyber.flink.Utils.getResourceAsString;
import static org.hamcrest.MatcherAssert.assertThat;

@Slf4j
@Ignore("Stuck test")
public class TestStixJob extends StixJob {
    protected ManualSource<String> source;
    protected CollectingSink<ThreatIntelligence> sink = new CollectingSink<>();
    protected CollectingSink<ThreatIntelligenceDetails> sinkDetails = new CollectingSink<>();

    protected ManualSource<Message> messageSource;
    protected CollectingSink<Message> resultsSink = new CollectingSink<>();


    @Test
    public void testStream() throws Exception {
        StreamExecutionEnvironment env = createPipeline(ParameterTool.fromMap(new HashMap<String, String>() {{
        }}));

        JobTester.startTest(env);

        //SpecificData.get().addLogicalTypeConversion(new Conversions.UUIDConversion());

        source.sendRecord(getResourceAsString("domain.xml"),0);
        source.sendRecord(getResourceAsString("domain2.xml"), 100);
        source.sendRecord(getResourceAsString("ip.xml"), 200);
        source.sendRecord(getResourceAsString("sample.xml"), 300);

        JobTester.stopTest();

        for (int i = 0; i < 4; i++) {
            ThreatIntelligence out = sink.poll();
            assertThat(String.format("Got a response for %d", i), out, Matchers.notNullValue());
            log.info(String.format("Response %d: %s", i, out));
        }
    }

    @Override
    protected MapFunction<Message, Message> getLongTermLookupFunction() {
        return (MapFunction<Message, Message>) message -> message;
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        results.addSink(resultsSink);
    }

    @Override
    protected void writeStixResults(ParameterTool params, DataStream<ThreatIntelligence> results) {
        results.addSink(sink);
    }

    @Override
    protected void writeDetails(ParameterTool params, DataStream<ThreatIntelligenceDetails> results) {
        results.addSink(sinkDetails);
    }

    @Override
    protected DataStream<String> createStixSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(String.class));
        return source.getDataStream();
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        messageSource = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return messageSource.getDataStream();
    }
}
