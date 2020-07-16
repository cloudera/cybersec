package com.cloudera.cyber.dedupe;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestDedupeLateDataJob extends DedupeJob{

    private ManualSource<Message> source;
    private CollectingSink<DedupeMessage> sink = new CollectingSink<>();
    private List<Message> recordLog = new ArrayList<>();

    @Test
    public void testDeduplicationWithCount() throws Exception {
//        long ts = new Date().getTime();
        long ts = 0;

        List<String> fields = Arrays.asList("a", "b");
        JobTester.startTest(createPipeline(ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAM_DEDUPE_KEY, "a,b");
            put(PARAM_DEDUPE_MAX_TIME, "1000");
            put(PARAM_DEDUPE_MAX_COUNT, "3");
        }})));

        // first set before watermark, 2 dupes, 1 non-dupe
        sendRecord(Message.builder().ts(ts + 100).fields(createFields(fields, "test")).build());
        sendRecord(Message.builder().ts(ts + 700).fields(createFields(fields, "test")).build());
        sendRecord(Message.builder().ts(ts + 500).fields(createFields(fields, "test2")).build());

        source.sendWatermark(ts + 1000);

        // late arrival dupe
        sendRecord(Message.builder().ts(ts + 500).fields(createFields(fields, "test")).build());
        // late arrival dupe of previous non-dupe
        sendRecord(Message.builder().ts(ts + 500).fields(createFields(fields, "test2")).build());

        // within window dupes
        sendRecord(Message.builder().ts(ts + 1100).fields(createFields(fields, "test")).build());
        sendRecord(Message.builder().ts(ts + 1700).fields(createFields(fields, "test")).build());

        source.sendWatermark(ts + 2000);

        assertThat(this.recordLog.stream().map(m->m.getTs()).max(Long::compareTo).get(), lessThan(ts + 3000));


        List<DedupeMessage> output = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            try{
                output.add(sink.poll(Duration.ofMillis(100)));
            } catch(TimeoutException e){
            }
        }
        output.forEach(this::checkResult);
        // note that the size will be 1 greater than expected due to the max count emitter which breaks
        // one of the 3 duplicates into a 2 and a 1.
        Stream<DedupeMessage> lateMessages = output.stream().filter(m -> m.isLate());

        assertThat("Result count", output, hasSize(5));
        assertThat("Late arrivals", lateMessages.count(), is(2L));

        JobTester.stopTest();
        assertTrue(sink.isEmpty());
    }

    private void sendRecord(Message d) {
        this.source.sendRecord(d, d.getTs());
        this.recordLog.add(d);
    }

    private void checkResult(DedupeMessage results) {
        assertThat("results have all fields", results.getFields(), allOf(hasKey("a"), hasKey(("b"))));
        assertThat("count cannot be above max size", results.getCount(), lessThanOrEqualTo(3L));
    }

    private Map<String, Object> createFields(List<String> fields, String value) {
        return new HashMap<String, Object>() {{
            fields.forEach(f -> put(f, value));
        }};
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<DedupeMessage> results) {
        results.addSink(sink);
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> sessionKey, Long sessionTimeout) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        return source.getDataStream();
    }
}

