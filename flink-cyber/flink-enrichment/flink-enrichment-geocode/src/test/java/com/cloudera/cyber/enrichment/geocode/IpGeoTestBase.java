package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.hamcrest.Matchers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

@Slf4j
public class IpGeoTestBase extends IpGeoJob {
    protected static final String STRING_IP_FIELD_NAME = "string_ip_field";
    protected final CollectingSink<Message> sink = new CollectingSink<>();
    protected final List<Message> recordLog = new ArrayList<>();
    protected final Map<Long, Map<String, String>> expectedExtensions = new HashMap<>();
    protected ManualSource<Message> source;

    protected void sendRecord(Message.MessageBuilder d) {
        Message r = d.id(UUID.randomUUID().toString())
                .originalSource(TestUtils.source("test", 0, 0))
                .build();
        this.source.sendRecord(r, r.getTs());
        this.recordLog.add(r);
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }

    @Override
    protected SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream().map(s -> s);
    }

    protected void checkResults() {

        List<Message> results = new ArrayList<>();

        int recordCount = recordLog.size();
        for (int i = 0; i < recordCount; i++) {
            try {
                results.add(sink.poll(Duration.ofMillis(100)));
            } catch (TimeoutException e) {
                log.info("Caught timeout exception.");
            }
        }

        for(Message result : results) {
            assertThat(String.format("extension fields match - index %d", result.getTs()), result.getExtensions(), Matchers.equalTo(expectedExtensions.get(result.getTs())));
        }
        assertThat("expected number of results returned", expectedExtensions.size(), Matchers.equalTo(results.size()));
        assertThat("Result count", results, hasSize(recordLog.size()));
    }

}
