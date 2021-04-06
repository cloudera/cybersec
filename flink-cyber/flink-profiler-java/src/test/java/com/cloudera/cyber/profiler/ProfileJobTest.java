package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.cloudera.cyber.flink.FlinkUtils.PARAMS_PARALLELISM;

public class ProfileJobTest extends ProfileJob {

    private static final String KEY_FIELD_NAME = "key_field";
    private static final String SUM_KEY_FIELD_NAME = "region";
    private static final String KEY_1 = "key1";
    private static final String KEY_2 = "key_2";
    private static final String MAX_FIELD_NAME = "max_field";
    private static final String SUM_FIELD_NAME = "sum_field";
    private static final String COUNT_PROFILE_EXTENSION = "count";
    private static final String MAX_PROFILE_EXTENSION = "max";
    private static final String TEST_PROFILE_GROUP = "test_profile";
    private static final String BYTE_COUNT_PROFILE_GROUP ="region_byte_count";
    private static final String REGION_1 = "region_1";
    private static final String REGION_2 = "region_2";
    private static final String SUM_PROFILE_EXTENSION = "total_bytes";

    private ManualSource<Message> source;

    private final CollectingSink<Message> sink = new CollectingSink<>();


    @Test
    public void testPipeline() throws Exception {
        Map<String, String> props = new HashMap<String, String>() {{
            put(PARAMS_PARALLELISM, "1");
            put(PARAM_PROFILE_CONFIG, ClassLoader.getSystemResource("test_profile.json").getPath());
        }};
        JobTester.startTest(createPipeline(ParameterTool.fromMap(props)));
        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        // send the messages to the any profile
        sendMaxMessage(currentTimestamp, KEY_1, 20);
        sendMaxMessage(currentTimestamp, KEY_2, 50);
        sendMaxMessage(currentTimestamp + 1, KEY_1, 10);
        sendMaxMessage(currentTimestamp + 1, KEY_2, 75);
        sendMaxMessage(currentTimestamp + 2, KEY_1, 30);

        sendSumMessage(currentTimestamp + 5, REGION_1, 1024);
        sendSumMessage(currentTimestamp + 6, REGION_1, 512);
        sendSumMessage(currentTimestamp + 4, REGION_2, 128);
        sendSumMessage(currentTimestamp + 2500, REGION_1, 1000);

        sendSumMessage(currentTimestamp + 3000, REGION_1, 50000);

        for(int i = 0; i < 5; i++) {
            sendSumMessage(currentTimestamp + 3000 + 5000 * i, REGION_1, 10000*i);
        }
        // send the messages to the bytes profile
        JobTester.stopTest();

        Map<String, Map<String, String>> expectedExtensions = new HashMap<String, Map<String, String>>() {{
            put(TEST_PROFILE_GROUP + KEY_1, new HashMap<String, String>() {{
                           put(COUNT_PROFILE_EXTENSION, "3");
                           put(MAX_PROFILE_EXTENSION, "30");
                           put(KEY_FIELD_NAME, KEY_1);
                           put(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, TEST_PROFILE_GROUP);
                           put(ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(currentTimestamp));
                           put(ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(currentTimestamp + 2));
                        }});

            put(TEST_PROFILE_GROUP + KEY_2, new HashMap<String, String>() {{
                    put(COUNT_PROFILE_EXTENSION, "2");
                    put(MAX_PROFILE_EXTENSION, "75");
                    put(KEY_FIELD_NAME, KEY_2);
                    put(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, TEST_PROFILE_GROUP);
                    put(ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(currentTimestamp));
                    put(ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(currentTimestamp + 1));
             }});

            put(BYTE_COUNT_PROFILE_GROUP + REGION_1, new HashMap<String, String>() {{
                put(SUM_PROFILE_EXTENSION, "1536");
                put(SUM_KEY_FIELD_NAME, REGION_1);
                put(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, BYTE_COUNT_PROFILE_GROUP);
                put(ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(currentTimestamp + 5));
                put(ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(currentTimestamp + 6));
            }});

            put(BYTE_COUNT_PROFILE_GROUP + REGION_2, new HashMap<String, String>() {{
                put(SUM_PROFILE_EXTENSION, "128");
                put(SUM_KEY_FIELD_NAME, REGION_2);
                put(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, BYTE_COUNT_PROFILE_GROUP);
                put(ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(currentTimestamp + 4));
                put(ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(currentTimestamp + 4));
            }});

        }};
       /* IntStream.range(0, 3).forEach(i -> {
            try {
                verifyProfileMessages(expectedExtensions);
            } catch (TimeoutException e) {
                throw new IllegalStateException("poll timed out");
            }
        }); */
        List<Message> messages = new ArrayList<>();
        while (!sink.isEmpty()) {
            messages.add(sink.poll());
        }

        // TODO: verify message content
        Assert.assertFalse(messages.isEmpty());
    }

    private void verifyProfileMessages(Map<String, Map<String, String>> expectedExtensions) throws TimeoutException {
        Message profile = sink.poll(Duration.ofSeconds(5));

        Map<String, String> extensions = profile.getExtensions();
        String expectedExtensionsKey = extensions.get(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION) + extensions.getOrDefault(KEY_FIELD_NAME, extensions.get(SUM_KEY_FIELD_NAME));
        Assert.assertEquals(expectedExtensions.get(expectedExtensionsKey), extensions);
    }

    private void sendMaxMessage(long timestamp, String keyFieldValue, long maxFieldValue) {
        Map<String, String> extensions = ImmutableMap.of(
                KEY_FIELD_NAME, keyFieldValue,
                MAX_FIELD_NAME, Long.toString(maxFieldValue)
        );
        Message message = TestUtils.createMessage(timestamp, "test", extensions);
        source.sendRecord(message);
    }

    private void sendSumMessage(long timestamp, String keyFieldValue, long sumFieldValue) {
        Map<String, String> extensions = ImmutableMap.of(
                SUM_KEY_FIELD_NAME, keyFieldValue,
                SUM_FIELD_NAME, Long.toString(sumFieldValue)
        );
        Message message = TestUtils.createMessage(timestamp, "netflow", extensions);
        source.sendRecord(message, timestamp);
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));
        WatermarkStrategy<Message> watermarkStrategy = WatermarkStrategy
                .<Message>forBoundedOutOfOrderness(Duration.ofMillis(1000))
                .withTimestampAssigner((message, timestamp) -> message.getTs());
        return source.getDataStream().assignTimestampsAndWatermarks(watermarkStrategy)
                .setParallelism(1);
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results, String profileGroupName) {
        results.addSink(sink).name("Profile events " + profileGroupName).setParallelism(1);
    }

    @Override
    protected DataStream<Message> updateFirstSeen(ParameterTool params, DataStream<Message> results, ProfileGroupConfig profileGroupConfig) {
        //skip hbase for unit tests
        return results;
    }

}
