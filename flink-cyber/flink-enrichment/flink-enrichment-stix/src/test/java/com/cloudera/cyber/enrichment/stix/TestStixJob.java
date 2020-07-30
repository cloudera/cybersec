package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.ThreatIntelligence;
import com.google.common.io.Resources;
import lombok.extern.java.Log;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import static com.cloudera.cyber.flink.Utils.getResourceAsString;
import static org.hamcrest.MatcherAssert.assertThat;

@Log
public class TestStixJob extends StixJob {
    protected ManualSource<String> source;
    protected CollectingSink<ThreatIntelligence> sink = new CollectingSink<>();
    protected CollectingSink<ThreatIntelligenceDetails> sinkDetails = new CollectingSink<>();


    protected ManualSource<Message> messageSource;
    protected CollectingSink<Message> resultsSink = new CollectingSink<>();


    @Test
    public void testStream() throws Exception {
        JobTester.startTest(createPipeline(ParameterTool.fromMap(new HashMap<String, String>() {{
        }})));

        source.sendRecord(getResourceAsString("domain.xml"));
        source.sendRecord(getResourceAsString("domain2.xml"));
        source.sendRecord(getResourceAsString("ip.xml"));
        source.sendRecord(getResourceAsString("sample.xml"));

        JobTester.stopTest();

        for (int i = 0; i < 10; i++) {
            ThreatIntelligence out = sink.poll();
            assertThat(String.format("Got a response for %d", i), out, Matchers.notNullValue());
            log.info(String.format("Response %d: %s", i, out));
        }

    }

    @Override
    protected MapFunction<Message, Message> getLongTermLookupFunction() {
        return new MapFunction<Message, Message>() {
            @Override
            public Message map(Message message) throws Exception {
                return message;
            }
        };
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
