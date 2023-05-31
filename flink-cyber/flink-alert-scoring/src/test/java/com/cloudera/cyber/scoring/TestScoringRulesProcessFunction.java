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

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.rules.DynamicRuleCommandType;
import com.cloudera.cyber.rules.RuleType;
import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.BroadcastOperatorTestHarness;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.cloudera.cyber.flink.FlinkUtils.PARAMS_PARALLELISM;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestScoringRulesProcessFunction {
    private static final String FIRST_RULE_EXTENSION_KEY = "test.key.1";
    private static final String SECOND_RULE_EXTENSION_KEY = "test.key.2";
    private static final String MATCH_REASON = "key match";
    private static final String NO_MATCH_REASON = "no match";
    private static final String RULE_SCRIPT_FORMAT = "if (message.containsKey(\"%s\")) {\n" +
        "        return {score: %f, reason: '"+ MATCH_REASON + "'};\n" +
        "  } else {\n" +
        "        return {score: 0.0, reason: '" + NO_MATCH_REASON + "'};\n" +
        "  }";

    private static final String RULE_SCRIPT_FORMAT_BROKEN = "if 111(message.containsKey(\"%s\")) {\n" +
            "        return {score: %f, reason: '"+ MATCH_REASON + "'};\n" +
            "  } else {\n" +
            "        return {score: 0.0, reason: '" + NO_MATCH_REASON + "'};\n" +
            "  }";

    @Test
    public void testUpsertRule() throws Exception {
        BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness = createTestHarness();

        // insert a new rule
        double expectedScore = 90.0;
        ScoringRule ruleV0 = buildScoringRule(true, 1, "first ruleV0", String.format(RULE_SCRIPT_FORMAT, FIRST_RULE_EXTENSION_KEY, expectedScore));
        ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults = verifyUpsert(ruleV0, false, harness, null);

        // send some messages to trigger the rule
        List<ScoredMessage> expectedScoredMessages = new ArrayList<>();
        sendMessage(expectedScoredMessages, new HashMap<String,String>() {{ put(FIRST_RULE_EXTENSION_KEY, "key value");}},
                Collections.singletonList(Scores.builder().ruleId(ruleV0.getId()).score(expectedScore).reason(MATCH_REASON).build()), harness);
        expectedScoredMessages.get(0).setCyberScore(expectedScore);
        sendMessage(expectedScoredMessages, Collections.emptyMap(),
                Collections.singletonList(Scores.builder().ruleId(ruleV0.getId()).score(0.0).reason(NO_MATCH_REASON).build()), harness);
        expectedScoredMessages.get(1).setCyberScore(0.0);

        // update the rule script
        expectedScore = 70.0;
        ScoringRule ruleV1 = ruleV0.toBuilder().ruleScript(String.format(RULE_SCRIPT_FORMAT, FIRST_RULE_EXTENSION_KEY, expectedScore)).build();
        verifyUpsert(ruleV1, true, harness, scoringRuleCommandResults);

        // trigger the rule again and make sure the results match the new version
        sendMessage(expectedScoredMessages, new HashMap<String,String>() {{ put(FIRST_RULE_EXTENSION_KEY, "key value");}},
                Collections.singletonList(Scores.builder().ruleId(ruleV0.getId()).score(expectedScore).reason(MATCH_REASON).build()), harness);
        expectedScoredMessages.get(2).setCyberScore(expectedScore);

        assertEquals(expectedScoredMessages, harness.extractOutputValues());
        harness.snapshot(1L, 1L);
    }

    @Test
    public void testUpsertRuleFailed() throws Exception {
        BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness = createTestHarness();

        // insert a new rule
        double expectedScore = 90.0;
        ScoringRule ruleV0 = buildScoringRule(true, 1, "first ruleV0", String.format(RULE_SCRIPT_FORMAT_BROKEN, FIRST_RULE_EXTENSION_KEY, expectedScore));
        verifyUpsert(ruleV0, false, harness, null, false);

    }

    @Test
    public void testEnableDisableRule() throws Exception {
        BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness = createTestHarness();

        // insert a new rule
        double firstExpectedScore = 90.0;
        ScoringRule firstRule = buildScoringRule(true, 1, "first ruleV0", String.format(RULE_SCRIPT_FORMAT, FIRST_RULE_EXTENSION_KEY, firstExpectedScore));
        ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults = verifyUpsert(firstRule, false, harness, null);

        double secondExpectedScore = 50.0;
        ScoringRule secondRule = buildScoringRule(true, 2, "second ruleV0", String.format(RULE_SCRIPT_FORMAT,SECOND_RULE_EXTENSION_KEY, secondExpectedScore ));
        verifyUpsert(secondRule, false, harness, scoringRuleCommandResults);

        // send a message to
        // trigger the rule
        Map<String, String> extensionsToTriggerRule = new HashMap<String,String>() {{ put(FIRST_RULE_EXTENSION_KEY, "key value");}};
        List<ScoredMessage> expectedScoredMessages = new ArrayList<>();
        List<Scores> allScores=Arrays.asList(Scores.builder().ruleId(firstRule.getId()).score(firstExpectedScore).reason(MATCH_REASON).build(),
                Scores.builder().ruleId(secondRule.getId()).score(0.0).reason(NO_MATCH_REASON).build());
        sendMessage(expectedScoredMessages, extensionsToTriggerRule, allScores, harness);

        // disable the rule
        verifyEnabledDisabled(firstRule, DynamicRuleCommandType.DISABLE, harness, scoringRuleCommandResults);

        // trigger the rule again - no scores should be returned
        sendMessage(expectedScoredMessages, extensionsToTriggerRule,
                Collections.singletonList(allScores.get(1)), harness);

        // enable the rule
        verifyEnabledDisabled(firstRule, DynamicRuleCommandType.ENABLE, harness, scoringRuleCommandResults);

        // triggering the rule again - scores should be returned
        sendMessage(expectedScoredMessages, extensionsToTriggerRule, allScores, harness);

        expectedScoredMessages.get(0).setCyberScore(firstExpectedScore);
        expectedScoredMessages.get(1).setCyberScore(0.0);
        expectedScoredMessages.get(2).setCyberScore(firstExpectedScore);
        assertEquals(expectedScoredMessages, harness.extractOutputValues());
    }

    @Test
    public void testIllegalScoreSummarization() {
            String badScoringMethod = "BAD_SCORING";
            Map<String, String> props = ImmutableMap.of(PARAMS_PARALLELISM, "1",
                    ScoringJobKafka.SCORING_SUMMATION_NAME(), badScoringMethod);

            assertThrows(IllegalArgumentException.class, () -> new ScoringProcessFunction(ScoringJob.Descriptors.rulesResultSink, ScoringJob.Descriptors.rulesState, ParameterTool.fromMap(props)),
                    String.format(ScoringProcessFunction.ILLEGAL_SUMMARIZATION_METHOD_ERROR_MESSAGE, badScoringMethod, ScoringSummarizationMode.legalSummarizationModes()));
   }

    @Test
    public void testScoreMessages() {
        testScoreMessage(TestUtils.createMessage(), Collections.emptyList(), 0.0, ScoringSummarizationMode.DEFAULT());

        List<Scores> scores = Arrays.asList(Scores.builder().ruleId("1").reason("reason 1").score(50.0).build(),
                Scores.builder().ruleId("2").reason("reason 2").score(50.0).build());

        testScoreMessage(TestUtils.createMessage(), scores, 100.00, ScoringSummarizationMode.SUM);
        testScoreMessage(TestUtils.createMessage(), scores, 50.00, ScoringSummarizationMode.MEAN);

        testScoreMessage(TestUtils.createMessage(), Collections.singletonList(Scores.builder().ruleId("1").reason("reason 1").score(50.0).build()), 50.0, ScoringSummarizationMode.MAX);
    }

    private void testScoreMessage(Message baseMessage, List<Scores> scoreDetails, double expectedScore, ScoringSummarizationMode mode) {
        ScoredMessage scoredMessage =  ScoringProcessFunction.scoreMessage(baseMessage, scoreDetails, mode);
        Assert.assertEquals(baseMessage, scoredMessage.getMessage());
        Assert.assertEquals(scoreDetails, scoredMessage.getCyberScoresDetails());
        Assert.assertEquals(expectedScore, scoredMessage.getCyberScore(), 0.01);
    }

    @Test
    public void testDeleteRule() throws Exception {
        BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness = createTestHarness();

        // insert a new rule
        double expectedScore = 90.0;
        ScoringRule rule = buildScoringRule(true, 1, "first ruleV0", String.format(RULE_SCRIPT_FORMAT, FIRST_RULE_EXTENSION_KEY, expectedScore));
        ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults = verifyUpsert(rule, false, harness, null);

        // send a message to trigger the rule
        Map<String, String> extensionsToTriggerRule = new HashMap<String,String>() {{ put(FIRST_RULE_EXTENSION_KEY, "key value");}};
        List<ScoredMessage> expectedScoredMessages = new ArrayList<>();
        sendMessage(expectedScoredMessages, extensionsToTriggerRule,
                Collections.singletonList(Scores.builder().ruleId(rule.getId()).score(expectedScore).reason(MATCH_REASON).build()), harness);

        verifyDelete(rule, rule.getId(), harness, scoringRuleCommandResults);

        // trigger the rule again and make sure the deleted rule is not triggered
        sendMessage(expectedScoredMessages, extensionsToTriggerRule,
                Collections.emptyList(), harness);

        // delete the rule again should be no op
        verifyDelete(null, rule.getId(), harness, scoringRuleCommandResults);

        // trigger the rule again and make sure the deleted rule is not triggered
        sendMessage(expectedScoredMessages, extensionsToTriggerRule,
                Collections.emptyList(), harness);

        expectedScoredMessages.get(0).setCyberScore(expectedScore);
        expectedScoredMessages.get(1).setCyberScore(0.0);
        expectedScoredMessages.get(2).setCyberScore(0.0);
        assertEquals(expectedScoredMessages, harness.extractOutputValues());
    }

    @Test
    public void testGetListRule() throws Exception {
        BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness = createTestHarness();

        // insert a new rule
        double expectedScore = 90.0;
        ScoringRule rule = buildScoringRule(true, 1, "first ruleV0", String.format(RULE_SCRIPT_FORMAT, FIRST_RULE_EXTENSION_KEY, expectedScore));

        // getting the rule before inserting returns null
        ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults = verifyGet(null, rule.getId(), harness, null);
        verifyList(null, harness, scoringRuleCommandResults);

        // insert the rule
        verifyUpsert(rule, false, harness, scoringRuleCommandResults);

        // send a message to trigger the rule
        Map<String, String> extensionsToTriggerRule = new HashMap<String,String>() {{ put(FIRST_RULE_EXTENSION_KEY, "key value");}};
        List<ScoredMessage> expectedScoredMessages = new ArrayList<>();
        sendMessage(expectedScoredMessages, extensionsToTriggerRule,
                Collections.singletonList(Scores.builder().ruleId(rule.getId()).score(expectedScore).reason(MATCH_REASON).build()), harness);

        verifyGet(rule, rule.getId(), harness, scoringRuleCommandResults);
        verifyList(rule, harness, scoringRuleCommandResults);

        // trigger the rule again - get should not change the rule set
        sendMessage(expectedScoredMessages, extensionsToTriggerRule,
                Collections.singletonList(Scores.builder().ruleId(rule.getId()).score(expectedScore).reason(MATCH_REASON).build()), harness);

        expectedScoredMessages.forEach(sm -> sm.setCyberScore(expectedScore));
        assertEquals(expectedScoredMessages, harness.extractOutputValues());
    }

    private void sendMessage(List<ScoredMessage> expectedScoredMessages, Map<String, String> extensions, List<Scores> expectedScores, BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness) throws Exception {
        Message sentMessage = TestUtils.createMessage(extensions);
        harness.processElement(new StreamRecord<>(sentMessage));
        expectedScoredMessages.add(ScoringProcessFunction.scoreMessage(sentMessage, expectedScores, ScoringSummarizationMode.DEFAULT()));
    }

    private BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> createTestHarness() throws Exception {
        BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness = ProcessFunctionTestHarnesses
                .forBroadcastProcessFunction(new ScoringProcessFunction(ScoringJob.Descriptors.rulesResultSink, ScoringJob.Descriptors.rulesState, ParameterTool.fromMap(new HashMap<>())), ScoringJob.Descriptors.rulesState);
        harness.open();

        return harness;
    }

    private ScoringRule buildScoringRule(boolean enabled, int order, String name, String ruleScript) {
        Instant now = Instant.now();

        return ScoringRule.builder().
                id(UUID.randomUUID().toString()).
                order(order).
                enabled(enabled).
                name(name).
                tsStart(now.minus(Duration.ofMinutes(5))).
                tsEnd(now.plus(Duration.ofMinutes(5))).
                type(RuleType.JS).
                ruleScript(ruleScript).build();
    }

    private  ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> verifyUpsert(ScoringRule rule, boolean isUpdate, BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness, ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults) throws Exception {
        return verifyUpsert(rule, isUpdate, harness, scoringRuleCommandResults, true);
    }

    private  ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> verifyUpsert(ScoringRule rule, boolean isUpdate, BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness, ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults, Boolean success) throws Exception {
        ScoringRuleCommand upsertCommand =  buildScoringRuleCommand(DynamicRuleCommandType.UPSERT, rule, rule.getId());
        int version = rule.getVersion();
        if (isUpdate) {
            version += 1;
        }
        ScoringRuleCommandResult expectedResult = ScoringRuleCommandResult.builder().cmdId(upsertCommand.getId()).success(success).rule(rule.withVersion(version)).build();
        return verifyBroadcast(upsertCommand, harness, scoringRuleCommandResults, expectedResult);
    }

    private  ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> verifyEnabledDisabled(ScoringRule rule, DynamicRuleCommandType type, BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness, ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults) throws Exception {
        ScoringRuleCommand disableCommand =  buildScoringRuleCommand(type, rule, rule.getId());
        boolean expectedEnabled = (type == DynamicRuleCommandType.ENABLE);
        ScoringRuleCommandResult expectedResult = ScoringRuleCommandResult.builder().cmdId(disableCommand.getId()).success(true).rule(rule.withEnabled(expectedEnabled)).build();
        return verifyBroadcast(disableCommand, harness, scoringRuleCommandResults, expectedResult);
    }

    private ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> verifyDelete(ScoringRule rule, String ruleIdToDelete, BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness, ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults) throws Exception {
        ScoringRuleCommand deleteCommand =  buildScoringRuleCommand(DynamicRuleCommandType.DELETE, null, ruleIdToDelete);
        return verifyBroadcast(deleteCommand, harness, scoringRuleCommandResults, ScoringRuleCommandResult.builder().cmdId(deleteCommand.getId()).success(true).rule(rule).build());
    }

    private ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> verifyGet(ScoringRule rule, String ruleId, BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness, ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults) throws Exception {
        ScoringRuleCommand getCommand =  buildScoringRuleCommand(DynamicRuleCommandType.GET, null, ruleId);
        return verifyBroadcast(getCommand, harness, scoringRuleCommandResults, ScoringRuleCommandResult.builder().cmdId(getCommand.getId()).success(true).rule(rule).build());
    }

    private ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> verifyList(ScoringRule rule, BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness, ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults) throws Exception {
        ScoringRuleCommand listCommand =  buildScoringRuleCommand(DynamicRuleCommandType.LIST, null, null);
        return verifyBroadcast(listCommand, harness, scoringRuleCommandResults, ScoringRuleCommandResult.builder().cmdId(listCommand.getId()).success(true).rule(rule).build());
    }

    private ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> verifyBroadcast(ScoringRuleCommand command, BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness,
                                                                                          ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults, ScoringRuleCommandResult expectedReply) throws Exception {
        return verifyBroadcast(command, harness, scoringRuleCommandResults, Collections.singletonList(expectedReply));
    }

    private ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> verifyBroadcast(ScoringRuleCommand command, BroadcastOperatorTestHarness<Message, ScoringRuleCommand, ScoredMessage> harness,
                                                                                          ConcurrentLinkedQueue<StreamRecord<ScoringRuleCommandResult>> scoringRuleCommandResults, List<ScoringRuleCommandResult> expectedReplies) throws Exception {
        harness.processBroadcastElement(new StreamRecord<>(command));
        if (scoringRuleCommandResults == null) {
            scoringRuleCommandResults = harness.getSideOutput(ScoringJob.Descriptors.rulesResultSink);
        }
        for(ScoringRuleCommandResult expectedReply : expectedReplies) {
            ScoringRuleCommandResult reply = scoringRuleCommandResults.remove().getValue();
            assertEquals(expectedReply, reply);
        }

        return scoringRuleCommandResults;
    }

    private ScoringRuleCommand buildScoringRuleCommand(DynamicRuleCommandType type, ScoringRule rule, String ruleId) {
        return ScoringRuleCommand.builder().
                id(UUID.randomUUID().toString()).
                ts(Instant.now().toEpochMilli()).id(UUID.randomUUID().toString()).
                type(type).
                rule(rule).
                ruleId(ruleId).
                headers(Collections.emptyMap()).
                build();
    }
}
