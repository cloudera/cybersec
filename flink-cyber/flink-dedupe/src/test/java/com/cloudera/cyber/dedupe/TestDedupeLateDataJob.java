package com.cloudera.cyber.dedupe;

import com.cloudera.cyber.Message;
import lombok.extern.java.Log;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Log
public class TestDedupeLateDataJob extends TestDedupeJob{

    private ManualSource<Message> source;
    private CollectingSink<DedupeMessage> sink = new CollectingSink<>();
    private List<Message> recordLog = new ArrayList<>();

    @Test
    @Ignore("Needs work on deterministic correctness")
    public void testDeduplicationWithLateness   () throws Exception {
//        long ts = new Date().getTime();
        long ts = 0;

        List<String> fields = Arrays.asList("a", "b");

        StreamExecutionEnvironment env = createPipeline(ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAM_DEDUPE_KEY, "a,b");
            put(PARAM_DEDUPE_MAX_TIME, "1000");
            put(PARAM_DEDUPE_MAX_COUNT, "3");
        }}));
        env.setParallelism(1);

        try {

            JobTester.startTest(env);

            createMessages(ts);

            assertThat(this.recordLog.stream().map(m -> m.getTs()).max(Long::compareTo).get(), lessThan(ts + 3000));

            List<DedupeMessage> output = new ArrayList<>();

            for (int i = 0; i < 15; i++) {
                try {
                    output.add(sink.poll(Duration.ofMillis(100)));
                } catch (TimeoutException e) {
                }
            }
            output.forEach(this::checkResult);
            // note that the size will be 1 greater than expected due to the max count emitter which breaks
            // one of the 3 duplicates into a 2 and a 1.
            List<DedupeMessage> lateMessages = output.stream().filter(m -> m.isLate()).collect(Collectors.toList());

            log.info(String.format("Output: %s; lateMessages: %s", output, lateMessages));
            assertThat("All messages were processed", output.parallelStream().map(t -> t.getCount()).collect(Collectors.summingLong(l -> l)), equalTo(recordLog.size() - lateMessages.size()));
            //assertThat("Result count", output, hasSize(4));
            //assertThat("Late arrivals", lateMessages.size(), is(2L));
        } finally {
            JobTester.stopTest();
            //assertTrue(sink.isEmpty());
        }


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

