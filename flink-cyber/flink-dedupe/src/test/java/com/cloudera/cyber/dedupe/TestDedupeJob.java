package com.cloudera.cyber.dedupe;


import com.cloudera.cyber.Message;
import lombok.extern.java.Log;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Ignore;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Log
public class TestDedupeJob extends DedupeJob {

    private ManualSource<Message> source;
    private CollectingSink<DedupeMessage> sink = new CollectingSink<>();
    private List<Message> recordLog = new ArrayList<>();

    @Test
    @Ignore("Needs work on deterministic correctness")
    public void testDeduplication() throws Exception {
//        long ts = new Date().getTime();
        long ts = 1000000;

        List<Message> messages = createRandomMessage(ts);

        try {
            JobTester.startTest(createPipeline(ParameterTool.fromMap(new HashMap<String, String>() {{
                put(PARAM_DEDUPE_KEY, "a,b");
                put(PARAM_DEDUPE_MAX_TIME, "1000");
                put(PARAM_DEDUPE_MAX_COUNT, "2");
                put(PARAM_DEDUPE_LATENESS, "100");
            }})).setParallelism(1));

            createMessages(ts);

            assertThat(this.recordLog.stream().map(m -> m.getTs()).max(Long::compareTo).get(), lessThan(ts + 3000));

            Map<GroupKey, Integer> expected = this.recordLog.stream()
                    .collect(Collectors.groupingBy(m -> {
                        return GroupKey.builder()
                                .a(m.getFields().get("a").toString())
                                .b(m.getFields().get("b").toString())
                                .ts((long) (Math.ceil(m.getTs() / 1000) * 1000 + 1000))
                                .build();
                    }))
                    .entrySet().stream()
                    .collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue().size(),
                            (v1, v2) -> v1 + v2,
                            HashMap::new));

            List<DedupeMessage> output = new ArrayList<>();

            for (int i = 0; i < 10; i++) {
//                try {
//                    //output.add(sink.poll(Duration.ofMillis(100)));
//                } catch (TimeoutException e) {
//                }
            }
            output.forEach(this::checkResult);
            // note that the size will be 1 greater than expected due to the max count emitter which breaks
            // one of the 3 duplicates into a 2 and a 1.
            List<DedupeMessage> lateMessages = output.stream().filter(m -> m.isLate()).collect(Collectors.toList());

            log.info(String.format("Output: %s; lateMessages: %s", output, lateMessages));
            assertThat("All messages were processed", output.parallelStream()
                    .map(t -> t.getCount())
                    .collect(Collectors.summingLong(l -> l)), equalTo((long) (recordLog.size())));

            /*
            TODO - work on correctness for this
            */
            assertThat("Result count", output, hasSize(expected.size()));
            assertThat("Late arrivals", lateMessages.size(), is(2L));


        } finally {
            JobTester.stopTest();
        }
    }

    public void createMessages(long ts) throws TimeoutException {
        Message.MessageBuilder MESSAGE_A = Message.builder()
                .fields(new HashMap<String, Object>() {{
                    put("a", "test");
                    put("b", "test");
                }});
        Message.MessageBuilder MESSAGE_B = Message.builder()
                .fields(new HashMap<String, Object>() {{
                    put("a", "test2");
                    put("b", "test2");
                }});
        Message.MessageBuilder MESSAGE_C = Message.builder()
                .fields(new HashMap<String, Object>() {{
                    put("a", "test3");
                    put("b", "test3");
                    put("c", "test3");
                }});

        sendRecord(MESSAGE_A.ts(ts + 0));
        sendRecord(MESSAGE_A.ts(ts + 100));
        sendRecord(MESSAGE_A.ts(ts + 200));
        sendRecord(MESSAGE_B.ts(ts + 200));
        sendRecord(MESSAGE_C.ts(ts + 200));

        DedupeMessage result1 = sink.poll(Duration.ofMillis(1000));
        assertThat("First duplicate received", result1.getCount(), equalTo(2L));
        assertThat("First duplicate received", result1.getStartTs(), equalTo(ts + 0L));
        assertThat("First duplicate received", result1.getTs(), equalTo(ts + 100L));

        source.sendWatermark(ts + 10000);
        System.out.println(sink.poll(Duration.ofMillis(11000)));
        //log.info(sink.poll(Duration.ofMillis(1000)).toString());


        // insert late message
        sendRecord(MESSAGE_A.ts(ts + 500));

        sendRecord(MESSAGE_A.ts(ts + 1100));
        sendRecord(MESSAGE_A.ts(ts + 1200));
        sendRecord(MESSAGE_B.ts(ts + 1300));
        sendRecord(MESSAGE_B.ts(ts + 1400));
        sendRecord(MESSAGE_C.ts(ts + 1500));

        source.sendWatermark(ts + 2000);

        // insert extremely late
        sendRecord(MESSAGE_A.ts(ts + 150));

        source.markFinished();
    }

    private void sendRecord(Message.MessageBuilder d) {
        Message r = d.id(UUID.randomUUID()).build();
        this.source.sendRecord(r, r.getTs());
        this.recordLog.add(r);
    }

    private void checkResult(DedupeMessage results) {
        assertThat("results have all fields", results.getFields(), allOf(hasKey("a"), hasKey(("b"))));
        assertThat("results does not have unused field", results.getFields(), not(hasKey("c")));
        assertThat("count cannot be above max size", results.getCount(), lessThanOrEqualTo(2L));
    }

    private List<Message> createRandomMessage(long ts) {
        return Stream.of(
                createFields(Arrays.asList("a", "b"), "test"),
                createFields(Arrays.asList("a", "b"), "test2"),
                createFields(Arrays.asList("a", "b", "c'"), "test3")
        ).map(fields ->
                Message.builder()
                        .id(UUID.randomUUID())
                        .ts(ts)
                        .fields(fields)
                        .build()
        ).collect(Collectors.toList());

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



