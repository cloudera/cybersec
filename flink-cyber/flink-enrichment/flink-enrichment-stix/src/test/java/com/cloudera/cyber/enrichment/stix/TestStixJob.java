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
import org.junit.Test;

import java.time.Duration;
import java.util.HashMap;

import static com.cloudera.cyber.flink.Utils.getResourceAsString;
import static org.hamcrest.MatcherAssert.assertThat;

@Slf4j
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

        source.sendRecord(getResourceAsString("domain.xml"));
        source.sendRecord(getResourceAsString("domain2.xml"));
        source.sendRecord(getResourceAsString("ip.xml"));
        source.sendRecord(getResourceAsString("sample.xml"));

        JobTester.stopTest();

        Thread.sleep(1000);
        for (int i = 0; i < 10; i++) {
            ThreatIntelligence out = sink.poll(Duration.ofMillis(10000));
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
