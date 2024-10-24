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

package com.cloudera.cyber.scoring;

import static com.cloudera.cyber.flink.FlinkUtils.PARAMS_PARALLELISM;
import static com.cloudera.cyber.rules.DynamicRuleCommandType.DELETE;
import static com.cloudera.cyber.rules.DynamicRuleCommandType.LIST;
import static com.cloudera.cyber.rules.DynamicRuleCommandType.UPSERT;
import static com.cloudera.cyber.rules.RuleType.JS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.rules.DynamicRuleCommandResult;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Test;

@Slf4j
public class TestScoringJob extends ScoringJob {

    private ManualSource<Message> source;
    private ManualSource<ScoringRuleCommand> querySource;

    private final CollectingSink<ScoredMessage> sink = new CollectingSink<>();
    private final CollectingSink<ScoringRuleCommandResult> queryResponse = new CollectingSink<>();

    private final List<Message> recordLog = new ArrayList<>();

    @Test
    public void testPipeline() throws Exception {
        Map<String, String> props = new HashMap<String, String>() {{
            put(PARAMS_PARALLELISM, "1");
        }};
        JobTester.startTest(createPipeline(ParameterTool.fromMap(props)));

        String ruleId = UUID.randomUUID().toString();
        String ruleName = "test-rule";
        ScoringRule rule = ScoringRule.builder()
              .id(ruleId)
              .name(ruleName)
              .tsStart(Instant.now())
              .tsEnd(Instant.now().plus(Duration.ofMinutes(5)))
              .order(0)
              .type(JS)
              .ruleScript("return { score: 1.0, reason: message.test }")
              .enabled(true)
              .build();

        sendRule(ScoringRuleCommand.builder()
              .type(UPSERT)
              .rule(rule)
              .ts(1L)
              .id(UUID.randomUUID().toString())
              .headers(Collections.emptyMap())
              .build());

        verifySuccessfulResponse(rule);

        // send a message and get score
        long scoredMessageTs = 100L;
        String expectedReason = "test-value";
        sendRecord(Message.builder()
              .ts(scoredMessageTs)
              .extensions(Collections.singletonMap("test", expectedReason))
              .originalSource(TestUtils.source("test", 0, 0))
              .source("test")
              .build());

        source.sendWatermark(100L);
        querySource.sendWatermark(100L);

        sendRule(ScoringRuleCommand.builder()
              .type(LIST).ts(900).id(UUID.randomUUID().toString()).headers(Collections.emptyMap()).build());

        source.sendWatermark(1000L);
        querySource.sendWatermark(1000L);

        verifyListResult(Collections.singletonList(rule));

        sendRule(ScoringRuleCommand.builder()
              .type(DELETE)
              .ts(1000L)
              .id(UUID.randomUUID().toString())
              .ruleId(ruleId).headers(Collections.emptyMap()).build());

        source.sendWatermark(1500L);
        querySource.sendWatermark(1500L);
        verifySuccessfulResponse(rule);


        long unscoredMessageTs = 2000L;
        sendRecord(Message.builder()
              .extensions(Collections.singletonMap("test", expectedReason))
              .ts(unscoredMessageTs)
              .originalSource(TestUtils.source("test", 0, 0))
              .source("test")
              .build());

        source.sendWatermark(3000L);
        querySource.sendWatermark(3000L);

        JobTester.stopTest();

        List<ScoredMessage> scoredMessages = collectScoredMessages();
        verifyMessageScores(scoredMessages, scoredMessageTs, 1.0,
              Collections.singletonList(Scores.builder().ruleId(ruleId).score(1.0).reason(expectedReason).build()));
        verifyMessageScores(scoredMessages, unscoredMessageTs, 0.0, Collections.emptyList());
    }

    @Test
    public void testPipelineCyberScore() throws Exception {
        Map<String, String> props = new HashMap<String, String>() {{
            put(PARAMS_PARALLELISM, "1");
            put(ScoringJobKafka.scoringSummationName(), ScoringSummarizationMode.SUM.name());
        }};
        JobTester.startTest(createPipeline(ParameterTool.fromMap(props)));

        String ruleId1 = UUID.randomUUID().toString();
        String ruleName1 = "test-rule";
        String ruleId2 = UUID.randomUUID().toString();
        String ruleName2 = "test-rule2";
        ScoringRule rule1 = ScoringRule.builder()
              .id(ruleId1)
              .name(ruleName1)
              .tsStart(Instant.now())
              .tsEnd(Instant.now().plus(Duration.ofMinutes(5)))
              .order(0)
              .type(JS)
              .ruleScript("return { score: 1.0, reason: message.test }")
              .enabled(true)
              .build();
        ScoringRule rule2 = ScoringRule.builder()
              .id(ruleId2)
              .name(ruleName2)
              .tsStart(Instant.now())
              .tsEnd(Instant.now().plus(Duration.ofMinutes(5)))
              .order(1)
              .type(JS)
              .ruleScript("return { score: 2.0, reason: message.test }")
              .enabled(true)
              .build();

        sendRule(ScoringRuleCommand.builder()
              .type(UPSERT)
              .rule(rule1)
              .ts(1L)
              .id(UUID.randomUUID().toString())
              .headers(Collections.emptyMap())
              .build());

        sendRule(ScoringRuleCommand.builder()
              .type(UPSERT)
              .rule(rule2)
              .ts(2L)
              .id(UUID.randomUUID().toString())
              .headers(Collections.emptyMap())
              .build());

        verifySuccessfulResponse(rule1);
        verifySuccessfulResponse(rule2);

        // send a message and get score
        long scoredMessageTs = 100L;
        String expectedReason = "test-value";
        sendRecord(Message.builder()
              .ts(scoredMessageTs)
              .extensions(Collections.singletonMap("test", expectedReason))
              .originalSource(TestUtils.source("test", 0, 0))
              .source("test")
              .build());

        source.sendWatermark(100L);
        querySource.sendWatermark(100L);

        sendRule(ScoringRuleCommand.builder()
              .type(LIST).ts(900).id(UUID.randomUUID().toString()).headers(Collections.emptyMap()).build());

        source.sendWatermark(1000L);
        querySource.sendWatermark(1000L);

        verifyListResult(Arrays.asList(rule1, rule2));

        sendRule(ScoringRuleCommand.builder()
              .type(DELETE)
              .ts(1000L)
              .id(UUID.randomUUID().toString())
              .ruleId(ruleId1).headers(Collections.emptyMap()).build());
        sendRule(ScoringRuleCommand.builder()
              .type(DELETE)
              .ts(1000L)
              .id(UUID.randomUUID().toString())
              .ruleId(ruleId2).headers(Collections.emptyMap()).build());

        source.sendWatermark(1500L);
        querySource.sendWatermark(1500L);
        verifySuccessfulResponse(rule1);
        verifySuccessfulResponse(rule2);


        long unscoredMessageTs = 2000L;
        sendRecord(Message.builder()
              .extensions(Collections.singletonMap("test", expectedReason))
              .ts(unscoredMessageTs)
              .originalSource(TestUtils.source("test", 0, 0))
              .source("test")
              .build());

        source.sendWatermark(3000L);
        querySource.sendWatermark(3000L);

        JobTester.stopTest();

        List<ScoredMessage> scoredMessages = collectScoredMessages();
        verifyMessageScores(scoredMessages, scoredMessageTs, 3.0,
              Arrays.asList(
                    Scores.builder().ruleId(ruleId1).score(1.0).reason(expectedReason).build(),
                    Scores.builder().ruleId(ruleId2).score(2.0).reason(expectedReason).build()));
        verifyMessageScores(scoredMessages, unscoredMessageTs, 0.0, Collections.emptyList());
    }

    void verifyMessageScores(List<ScoredMessage> scoredMessages, long originalMessageTimestamp,
                             double expectedCyberScore, List<Scores> expectedScores) {
        final Optional<ScoredMessage> scoredMessage = scoredMessages.stream()
              .filter((m) -> (m.getTs() == originalMessageTimestamp))
              .findFirst();
        List<Scores> actualScores = scoredMessage
              .map(ScoredMessage::getCyberScoresDetails)
              .orElse(Collections.emptyList());
        assertThat("message scores match", expectedScores, equalTo(actualScores));
        assertThat(scoredMessage.isPresent(), equalTo(true));
        assertThat("message scores match", scoredMessage.get().getCyberScore(), equalTo(expectedCyberScore));
    }

    private void verifySuccessfulResponse(ScoringRule expectedRule) throws TimeoutException {
        DynamicRuleCommandResult<ScoringRule> poll = queryResponse.poll();
        assertThat("Command succeed", poll.isSuccess());
        assertThat("rule matched", poll.getRule(), equalTo(expectedRule));
    }

    private void verifyListResult(List<ScoringRule> scoringRules) throws TimeoutException {
        for (ScoringRule expectedRule : scoringRules) {
            verifySuccessfulResponse(expectedRule);
        }
        verifySuccessfulResponse(null);
    }

    private void sendRecord(Message d) {
        this.source.sendRecord(d, d.getTs());
        this.recordLog.add(d);
    }

    private void sendRule(ScoringRuleCommand c) {
        this.querySource.sendRecord(c, c.getTs());
    }

    private List<ScoredMessage> collectScoredMessages() {
        List<ScoredMessage> output = new ArrayList<>();

        int recordCount = recordLog.size();
        for (int i = 0; i < recordCount; i++) {
            try {
                output.add(sink.poll(Duration.ofMillis(100)));
            } catch (TimeoutException e) {
                log.info("Caught timeout exception.");
            }
        }

        return output;
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<ScoredMessage> results) {
        results.addSink(sink).name("Scored messages").setParallelism(1);
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));

        return source.getDataStream();
    }

    @Override
    protected DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params) {
        querySource = JobTester.createManualSource(env, TypeInformation.of(ScoringRuleCommand.class));

        return querySource.getDataStream();
    }

    @Override
    protected void writeQueryResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results) {
        results.addSink(queryResponse).name("Scoring command Results").setParallelism(1);
    }

}
