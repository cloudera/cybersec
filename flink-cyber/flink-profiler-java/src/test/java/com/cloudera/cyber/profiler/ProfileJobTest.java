package com.cloudera.cyber.profiler;

import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.Scores;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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

import javax.print.DocFlavor;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import static com.cloudera.cyber.flink.FlinkUtils.PARAMS_PARALLELISM;

public class ProfileJobTest extends ProfileJob {

    private static final String KEY_FIELD_NAME = "key_field";
    private static final String SUM_KEY_FIELD_NAME = "region";
    private static final String SUM_SECOND_KEY_FIELD_NAME = "ip_src";
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
    private static final String IP_SRC_1 = "1.1.1.1";
    private static final String IP_SRC_2 = "2.2.2.2";
    private static final String IP_SRC_3 = "3.3.3.3";
    private static final String SUM_PROFILE_EXTENSION = "total_bytes";
    private static final String SUM_SCORES_PROFILE_EXTENSION = "total_score";
    private static final String SUM_SCORES_PROFILE_GROUP = "endpoint_aggregate_score";
    private static final String RULE_UUID = UUID.randomUUID().toString();
    private static final List<String> PROFILE_KEY_FIELDS = Lists.newArrayList(SUM_KEY_FIELD_NAME, SUM_SECOND_KEY_FIELD_NAME, KEY_FIELD_NAME);
    private ManualSource<ScoredMessage> source;

    private final CollectingSink<ScoredMessage> sink = new CollectingSink<>();


    @Test
    public void testPipeline() throws Exception {
        Map<String, String> props = new HashMap<String, String>() {{
            put(PARAMS_PARALLELISM, "1");
            put(PARAM_PROFILE_CONFIG, ClassLoader.getSystemResource("test_profile.json").getPath());
            put(PARAM_LATENESS_TOLERANCE_MILLIS, "5");
        }};
        JobTester.startTest(createPipeline(ParameterTool.fromMap(props)));
        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        // send the messages to the any profile
        sendMaxMessage(currentTimestamp, KEY_1, 20);
        sendMaxMessage(currentTimestamp, KEY_2, 50);
        sendMaxMessage(currentTimestamp + 1, KEY_1, 10);
        sendMaxMessage(currentTimestamp + 1, KEY_2, 75);
        sendMaxMessage(currentTimestamp + 2, KEY_1, 30);

        sendSumMessage(currentTimestamp + 5, REGION_1, IP_SRC_1, 1024);
        sendSumMessage(currentTimestamp + 6, REGION_1, IP_SRC_1, 512);
        sendSumMessage(currentTimestamp + 4, REGION_2, IP_SRC_2,128);
        sendSumMessage(currentTimestamp + 2500, REGION_1, IP_SRC_3, 1000);

        sendSumMessage(currentTimestamp + 3000, REGION_1, IP_SRC_3, 50000);

        // send the messages to the bytes profile
        JobTester.stopTest();

        ImmutableMap<String, Map<String, String>> expectedExtensions = ImmutableMap.<String, Map<String, String>>builder().
            put(TEST_PROFILE_GROUP + KEY_1, ImmutableMap.<String, String>builder().
                           put(COUNT_PROFILE_EXTENSION, "3").
                           put(MAX_PROFILE_EXTENSION, "30").
                           put(KEY_FIELD_NAME, KEY_1).
                           put(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, TEST_PROFILE_GROUP).
                           put(ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(currentTimestamp)).
                           put(ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(currentTimestamp + 2)).
                           build()).


            put(TEST_PROFILE_GROUP + KEY_2, ImmutableMap.<String, String>builder().
                    put(COUNT_PROFILE_EXTENSION, "2").
                    put(MAX_PROFILE_EXTENSION, "75").
                    put(KEY_FIELD_NAME, KEY_2).
                    put(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, TEST_PROFILE_GROUP).
                    put(ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(currentTimestamp)).
                    put(ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(currentTimestamp + 1)).
                    build()).

            put(BYTE_COUNT_PROFILE_GROUP + REGION_1, ImmutableMap.<String, String>builder().
                put(SUM_PROFILE_EXTENSION, "1536").
                put(SUM_KEY_FIELD_NAME, REGION_1).
                put(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, BYTE_COUNT_PROFILE_GROUP).
                put(ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(currentTimestamp + 5)).
                put(ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(currentTimestamp + 6)).
                build()).

            put(BYTE_COUNT_PROFILE_GROUP + REGION_2, ImmutableMap.<String, String>builder().
                put(SUM_PROFILE_EXTENSION, "128").
                put(SUM_KEY_FIELD_NAME, REGION_2).
                put(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, BYTE_COUNT_PROFILE_GROUP).
                put(ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(currentTimestamp + 4)).
                put(ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(currentTimestamp + 4)).
                build()).
            put(SUM_SCORES_PROFILE_GROUP + IP_SRC_1, ImmutableMap.<String, String>builder().
                    put(SUM_SCORES_PROFILE_EXTENSION, "14").
                    put(SUM_SECOND_KEY_FIELD_NAME, IP_SRC_1).
                    put(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, SUM_SCORES_PROFILE_GROUP).
                    put(ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(currentTimestamp + 5)).
                    put(ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(currentTimestamp + 6)).
                    build()).
            put(SUM_SCORES_PROFILE_GROUP + IP_SRC_2, ImmutableMap.<String, String>builder().
                    put(SUM_SCORES_PROFILE_EXTENSION, "13").
                    put(SUM_SECOND_KEY_FIELD_NAME, IP_SRC_2).
                    put(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, SUM_SCORES_PROFILE_GROUP).
                    put(ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(currentTimestamp + 4)).
                    put(ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(currentTimestamp + 4)).
                    build()).
            put(SUM_SCORES_PROFILE_GROUP + IP_SRC_3, ImmutableMap.<String, String>builder().
                    put(SUM_SCORES_PROFILE_EXTENSION, "62").
                    put(SUM_SECOND_KEY_FIELD_NAME, IP_SRC_3).
                    put(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, SUM_SCORES_PROFILE_GROUP).
                    put(ProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(currentTimestamp + 2500)).
                    put(ProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(currentTimestamp + 3000)).
                    build()).
            build();

       /* IntStream.range(0, 10).forEach(i -> {
            try {
                verifyProfileMessages(expectedExtensions);
            } catch (TimeoutException e) {
                throw new IllegalStateException("poll timed out");
            }
        }); */
        List<ScoredMessage> messages = new ArrayList<>();
        while (!sink.isEmpty()) {
            messages.add(sink.poll());
        }

        Assert.assertFalse(messages.isEmpty());
    }

    private void verifyProfileMessages(Map<String, Map<String, String>> expectedExtensions) throws TimeoutException {
        ScoredMessage profile = sink.poll(Duration.ofSeconds(5));

        Map<String, String> extensions = profile.getMessage().getExtensions();
        String keyFieldValue = extensions.entrySet().stream().filter(e -> PROFILE_KEY_FIELDS.contains(e.getKey())).
                map(e -> e.getValue()).findFirst().orElseThrow(() -> new RuntimeException("no key field found in profile message"));
        String expectedExtensionsKey = extensions.get(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION) + keyFieldValue;
        Assert.assertEquals(String.format("Message with key %s extensions did not match", expectedExtensionsKey), expectedExtensions.get(expectedExtensionsKey), extensions);
    }

    private void sendMaxMessage(long timestamp, String keyFieldValue, long maxFieldValue) {
        Map<String, String> extensions = ImmutableMap.of(
                KEY_FIELD_NAME, keyFieldValue,
                MAX_FIELD_NAME, Long.toString(maxFieldValue)
        );
        ScoredMessage message = ScoredMessage.builder().cyberScoresDetails( Collections.emptyList()).message(TestUtils.createMessage(timestamp, "test", extensions)).build();
        source.sendRecord(message);
    }

    private void sendSumMessage(long timestamp, String keyFieldValue, String secondKeyField, long sumFieldValue) {
        Map<String, String> extensions = ImmutableMap.of(
                SUM_KEY_FIELD_NAME, keyFieldValue,
                SUM_SECOND_KEY_FIELD_NAME, secondKeyField,
                SUM_FIELD_NAME, Long.toString(sumFieldValue)
        );

        Map<String, Double> ipToScore = ImmutableMap.of(IP_SRC_1, 7.0,
                                                        IP_SRC_2, 13.0,
                                                        IP_SRC_3, 31.0);

        List<Scores> scores = Collections.singletonList(Scores.builder().ruleId(RULE_UUID).reason("my reason").score(ipToScore.get(secondKeyField)).build());
        ScoredMessage message = ScoredMessage.builder().cyberScoresDetails(scores).
                message(TestUtils.createMessage(timestamp, "netflow", extensions)).build();
        source.sendRecord(message, timestamp);
    }

    @Override
    protected DataStream<ScoredMessage> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(ScoredMessage.class));
        WatermarkStrategy<ScoredMessage> watermarkStrategy = WatermarkStrategy
                .<ScoredMessage>forBoundedOutOfOrderness(Duration.ofMillis(1000))
                .withTimestampAssigner((message, timestamp) -> message.getTs());
        return source.getDataStream().assignTimestampsAndWatermarks(watermarkStrategy)
                .setParallelism(1);
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<ScoredMessage> results, String profileGroupName) {
        results.addSink(sink).name("Profile events " + profileGroupName).setParallelism(1);
    }

    @Override
    protected DataStream<ProfileMessage> updateFirstSeen(ParameterTool params, DataStream<ProfileMessage> results, ProfileGroupConfig profileGroupConfig) {
        //skip hbase for unit tests
        return results;
    }

}
