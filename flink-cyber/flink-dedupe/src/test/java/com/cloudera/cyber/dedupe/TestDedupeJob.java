package com.cloudera.cyber.dedupe;


import com.cloudera.cyber.Message;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestDedupeJob extends DedupeJob {

    private ManualSource<Message> source;
    private CollectingSink<DedupeMessage> sink = new CollectingSink<>();
    private List<Message> recordLog = new ArrayList<>();

    @Test
    public void testDeduplicationWithCount() throws Exception {
//        long ts = new Date().getTime();
        long ts = 0;

        List<Message> messages = createRandomMessage(ts);

        JobTester.startTest(createPipeline(ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAM_DEDUPE_KEY, "a,b");
            put(PARAM_DEDUPE_MAX_TIME, "1000");
            put(PARAM_DEDUPE_MAX_COUNT, "2");
        }})));

        messages.forEach(d -> sendRecord(d));
        messages.stream()
                .filter(d -> d.getFields().get("a").equals("test"))
                .map(m -> m.toBuilder().ts(m.getTs() + 900).build())
                .forEach(d -> sendRecord(d));
        source.sendWatermark(ts + 1000);

        messages.stream()
                .filter(d -> d.getFields().get("a").equals("test"))
                .map(m -> m.toBuilder().ts(m.getTs() + 1400).build())
                .forEach(d -> sendRecord(d));

        messages.stream()
                .filter(d -> d.getFields().get("a").equals("test"))
                .map(m -> m.toBuilder().ts(m.getTs() + 1500).build())
                .forEach(d -> sendRecord(d));

        messages.stream()
                .filter(d -> d.getFields().get("a").equals("test"))
                .map(m -> m.toBuilder().ts(m.getTs() + 1600).build())
                .forEach(d -> sendRecord(d));

        messages.stream()
                .filter(d -> !d.getFields().get("a").equals("test3"))
                .map(m -> m.toBuilder().ts(m.getTs() + 2500).build())
                .forEach(d -> sendRecord(d));

        source.sendWatermark(ts + 2000);

        messages.stream()
                .filter(d -> d.getFields().get("a").equals("test"))
                .map(m -> m.toBuilder().ts(m.getTs() + 2750).build())
                .forEach(d -> sendRecord(d));

        source.sendWatermark(ts + 3000);

        assertThat(this.recordLog.stream().map(m->m.getTs()).max(Long::compareTo).get(), lessThan(ts + 3000));

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


        Map<GroupKey, List<Message>> messageGroups = this.recordLog.stream()
                .collect(Collectors.groupingBy(m -> {
                    return GroupKey.builder()
                            .a(m.getFields().get("a").toString())
                            .b(m.getFields().get("b").toString())
                            .ts((long) (Math.ceil(m.getTs() / 1000) * 1000 + 1000))
                            .build();
                }));

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
        assertThat("Output has expected count of results", output, hasSize(expected.size() + 1));

        JobTester.stopTest();
        assertTrue(sink.isEmpty());
    }

    private void sendRecord(Message d) {
        this.source.sendRecord(d, d.getTs());
        this.recordLog.add(d);
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



