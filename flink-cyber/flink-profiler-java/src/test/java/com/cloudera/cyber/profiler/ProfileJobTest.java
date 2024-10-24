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

package com.cloudera.cyber.profiler;

import static com.cloudera.cyber.flink.FlinkUtils.PARAMS_PARALLELISM;
import static com.cloudera.cyber.profiler.ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION;
import static com.cloudera.cyber.profiler.StatsProfileAggregateFunction.STATS_PROFILE_GROUP_SUFFIX;
import static com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAcc.END_PERIOD_EXTENSION;
import static com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAcc.START_PERIOD_EXTENSION;
import static com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAcc.STATS_EXTENSION_SUFFIXES;
import static com.cloudera.cyber.rules.DynamicRuleCommandType.UPSERT;
import static com.cloudera.cyber.rules.RuleType.JS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.withPrecision;
import static org.hamcrest.Matchers.equalTo;

import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.profiler.dto.ProfileDto;
import com.cloudera.cyber.rules.DynamicRuleCommandResult;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.Scores;
import com.cloudera.cyber.scoring.ScoringProcessFunction;
import com.cloudera.cyber.scoring.ScoringRule;
import com.cloudera.cyber.scoring.ScoringRuleCommand;
import com.cloudera.cyber.scoring.ScoringRuleCommandResult;
import com.cloudera.cyber.scoring.ScoringSummarizationMode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class ProfileJobTest extends ProfileJob {

    private static final String KEY_FIELD_NAME = "key_field";
    private static final String SUM_KEY_FIELD_NAME = "region";
    private static final String SUM_SECOND_KEY_FIELD_NAME = "ip_src";
    private static final String KEY_1 = "key1";
    private static final String KEY_2 = "key_2";
    private static final String MAX_FIELD_NAME = "max_field";
    private static final String SUM_FIELD_NAME = "sum_field";
    private static final String TEST_PROFILE_GROUP = "test_profile";
    private static final String BYTE_COUNT_PROFILE_GROUP = "region_byte_count";
    private static final String REGION_1 = "region_1";
    private static final String REGION_2 = "region_2";
    private static final String IP_SRC_1 = "1.1.1.1";
    private static final String IP_SRC_2 = "2.2.2.2";
    private static final String IP_SRC_3 = "3.3.3.3";

    private static final String SUM_SCORES_PROFILE_GROUP = "endpoint_aggregate_score";
    private static final String RULE_UUID = UUID.randomUUID().toString();
    private ManualSource<ScoredMessage> source;

    private final CollectingSink<ScoredMessage> sink = new CollectingSink<>();
    private ManualSource<ScoringRuleCommand> scoringRuleCommandSource;
    private final CollectingSink<ScoringRuleCommandResult> scoringRuleCommandResponse = new CollectingSink<>();
    private final CollectingSink<ProfileMessage> jdbcSink = new CollectingSink<>();

    @Test
    public void testDuplicateProfile() {
        String profileConfigFilePath = ClassLoader.getSystemResource("duplicate_profile.json").getPath();
        ImmutableMap<String, String> props = ImmutableMap.<String, String>builder()
                .put(PARAMS_PARALLELISM, "1")
                .put(PARAM_PROFILE_CONFIG, profileConfigFilePath)
                .put(PARAM_LATENESS_TOLERANCE_MILLIS, "5")
                .put(PARAMS_PHOENIX_DB_INIT, "false")
                .build();

        assertThatThrownBy(() -> createPipeline(ParameterTool.fromMap(props))).isInstanceOf(IllegalStateException.class).hasMessageContaining("Duplicate profile group names");
    }

    @Test
    public void testPipeline() throws Exception {
        String profileConfigFilePath = ClassLoader.getSystemResource("test_profile.json").getPath();
        ImmutableMap<String, String> props = ImmutableMap.<String, String>builder()
                .put(PARAMS_PARALLELISM, "1")
                .put(PARAM_PROFILE_CONFIG, profileConfigFilePath)
                .put(PARAM_LATENESS_TOLERANCE_MILLIS, "5")
                .put(PARAMS_PHOENIX_DB_INIT, "false")
                .build();


        List<ProfileGroupConfig> profileGroupConfigs = parseConfigFile(new String(Files.readAllBytes(Paths.get(profileConfigFilePath))));
        JobTester.startTest(createPipeline(ParameterTool.fromMap(props)));
        long currentTimestamp = MessageUtils.getCurrentTimestamp();

        ScoringRule rule = upsertScoringCommand();

        // send the messages to the any profile
        sendMaxMessage(currentTimestamp, KEY_1, 20);
        sendMaxMessage(currentTimestamp, KEY_2, 50);
        sendMaxMessage(currentTimestamp + 1, KEY_1, 10);
        sendMaxMessage(currentTimestamp + 1, KEY_2, 75);
        sendMaxMessage(currentTimestamp + 2, KEY_1, 30);

        sendSumMessage(currentTimestamp + 5, REGION_1, IP_SRC_1, 1024);
        sendSumMessage(currentTimestamp + 6, REGION_1, IP_SRC_1, 512);
        sendSumMessage(currentTimestamp + 4, REGION_2, IP_SRC_2, 128);
        sendSumMessage(currentTimestamp + 2500, REGION_1, IP_SRC_3, 1000);

        sendSumMessage(currentTimestamp + 6000, REGION_1, IP_SRC_3, 50000);

        // send the messages to the bytes profile
        JobTester.stopTest();

        ImmutableMap<String, List<String>> possibleKeyValues = ImmutableMap.<String, List<String>>builder()
                .put(TEST_PROFILE_GROUP, Lists.newArrayList(KEY_1, KEY_2))
                .put(SUM_SCORES_PROFILE_GROUP, Lists.newArrayList(IP_SRC_1, IP_SRC_2, IP_SRC_3))
                .put(BYTE_COUNT_PROFILE_GROUP, Lists.newArrayList(REGION_1, REGION_2))
                .build();


        List<ScoredMessage> messages = new ArrayList<>();
        while (!sink.isEmpty()) {
            messages.add(sink.poll());
        }

        List<ProfileMessage> jdbcMessages = new ArrayList<>();
        while (!jdbcSink.isEmpty()) {
            jdbcMessages.add(jdbcSink.poll());
        }

        assertThat(messages).isNotEmpty();
        assertThat(jdbcMessages).isNotEmpty();

        messages.forEach(message -> verifyProfileMessages(currentTimestamp, message, profileGroupConfigs, possibleKeyValues, rule));
        IntStream.range(0, 10).forEach(i -> verifyProfileMessages(currentTimestamp, messages.get(i), profileGroupConfigs, possibleKeyValues, rule));
    }

    private void verifyProfileMessages(long currentTimestamp, ScoredMessage profile, List<ProfileGroupConfig> profileGroupConfigs, Map<String, List<String>> possibleKeyValues, ScoringRule rule) {
        Map<String, String> extensions = profile.getMessage().getExtensions();

        // check profile group
        String profileGroupName = extensions.get(PROFILE_GROUP_NAME_EXTENSION);
        assertThat(profileGroupName).isNotNull();
        String baseProfileName = profileGroupName.replace(STATS_PROFILE_GROUP_SUFFIX, "");

        // find the profile definition
        ProfileGroupConfig profileGroupConfig = profileGroupConfigs.stream().filter(pg -> pg.getProfileGroupName().equals(baseProfileName)).findFirst().orElse(null);
        assertThat(profileGroupConfig).isNotNull();

        // check the start and end period and timestamp range
        long maxTimestamp = currentTimestamp + 6000;
        long startPeriod = Long.parseLong(extensions.get(START_PERIOD_EXTENSION));
        long endPeriod = Long.parseLong(extensions.get(END_PERIOD_EXTENSION));
        assertThat(startPeriod).isBetween(currentTimestamp, maxTimestamp);
        assertThat(startPeriod).isLessThanOrEqualTo(endPeriod);
        assertThat(endPeriod).isBetween(currentTimestamp, maxTimestamp);
        assertThat(profile.getTs()).isBetween(currentTimestamp, maxTimestamp);

        if (profileGroupName.endsWith(STATS_PROFILE_GROUP_SUFFIX)) {
            assertThat(profileGroupConfig.hasStats()).isTrue();
        } else {
            String keyExtensionName = profileGroupConfig.getKeyFieldNames().get(0);
            String keyValue = extensions.get(keyExtensionName);
            assertThat(possibleKeyValues.get(profileGroupName)).contains(keyValue);

            // make sure there is a value for each measurement and that it is a double
            profileGroupConfig.getMeasurements().forEach(m -> checkMeasurementValues(extensions, profileGroupConfig, m));
        }

        assertThat(profile.getCyberScore()).isCloseTo(1.0, withPrecision(.01d));
        assertThat(profile.getCyberScoresDetails()).hasSize(1).contains(Scores.builder().ruleId(rule.getId()).score(1.0).reason("profile").build());
    }

    private void checkMeasurementValues(Map<String, String> extensions, ProfileGroupConfig profileGroupConfig, ProfileMeasurementConfig measurement) {
        checkMeasurementValue(extensions, measurement.getResultExtensionName());
        if (profileGroupConfig.hasStats()) {
            profileGroupConfig.getMeasurements().forEach(m -> STATS_EXTENSION_SUFFIXES.forEach(suffix -> checkMeasurementValue(extensions, m.getResultExtensionName().concat(suffix))));
        }
    }

    private void checkMeasurementValue(Map<String, String> extensions, String name) {
        String measurementString = extensions.get(name);
        assertThat(measurementString).isNotNull();
        //noinspection ResultOfMethodCallIgnored
        assertThatCode(() -> Double.parseDouble(measurementString)).doesNotThrowAnyException();
    }

    private void sendMaxMessage(long timestamp, String keyFieldValue, long maxFieldValue) {
        Map<String, String> extensions = ImmutableMap.of(
                KEY_FIELD_NAME, keyFieldValue,
                MAX_FIELD_NAME, Long.toString(maxFieldValue)
        );
        ScoredMessage message = ScoringProcessFunction.scoreMessage(TestUtils.createMessage(timestamp, "test", extensions), Collections.emptyList(), ScoringSummarizationMode.SUM);
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
        ScoredMessage message = ScoringProcessFunction.scoreMessage(TestUtils.createMessage(timestamp, "netflow", extensions), scores, ScoringSummarizationMode.defaultValue());
        source.sendRecord(message, timestamp);
    }

    @Override
    protected void writeProfileMeasurementsResults(ParameterTool params, List<ProfileDto> profileDtos, DataStream<ProfileMessage> results) {
        results.addSink(jdbcSink).name("Profile data ").setParallelism(1);
    }

    private ScoringRule upsertScoringCommand() throws TimeoutException {

        String ruleId = UUID.randomUUID().toString();
        String ruleName = "test-rule";
        ScoringRule rule = ScoringRule.builder()
                .id(ruleId)
                .name(ruleName)
                .tsStart(Instant.now())
                .tsEnd(Instant.now().plus(Duration.ofMinutes(5)))
                .order(0)
                .type(JS)
                .ruleScript("return { score: 1.0, reason: message.source }")
                .enabled(true)
                .build();

        ScoringRuleCommand command = ScoringRuleCommand.builder()
                .type(UPSERT)
                .rule(rule)
                .ts(1L)
                .id(UUID.randomUUID().toString())
                .headers(Collections.emptyMap())
                .build();
        scoringRuleCommandSource.sendRecord(command, command.getTs());
        scoringRuleCommandSource.sendWatermark(command.getTs() + 5000L);

        verifySuccessfulResponse(rule);

        return rule;
    }

    private void verifySuccessfulResponse(ScoringRule expectedRule) throws TimeoutException {
        DynamicRuleCommandResult<ScoringRule> poll = scoringRuleCommandResponse.poll();
        MatcherAssert.assertThat("Command succeed", poll.isSuccess());
        MatcherAssert.assertThat("rule matched", poll.getRule(), equalTo(expectedRule));
    }

    @Override
    protected DataStream<ScoredMessage> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(ScoredMessage.class));
        return source.getDataStream();
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<ScoredMessage> results) {
        results.addSink(sink).name("Profile events ").setParallelism(1);
    }

    @Override
    protected DataStream<ProfileMessage> updateFirstSeen(ParameterTool params, DataStream<ProfileMessage> results, ProfileGroupConfig profileGroupConfig) {
        //skip hbase for unit tests
        return results;
    }

    @Override
    protected DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params) {
        scoringRuleCommandSource = JobTester.createManualSource(env, TypeInformation.of(ScoringRuleCommand.class));

        return scoringRuleCommandSource.getDataStream();
    }

    @Override
    protected void writeScoredRuleCommandResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results) {
        results.addSink(scoringRuleCommandResponse).name("Kafka Score Rule Command Results").uid("kafka.output.rule.command.results");
    }

}
