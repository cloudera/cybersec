package com.cloudera.cyber.scoring;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.flink.MessageBoundedOutOfOrder;
import com.cloudera.cyber.rules.DynamicRuleCommandResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static com.cloudera.cyber.flink.FlinkUtils.PARAMS_PARALLELISM;
import static com.cloudera.cyber.rules.DynamicRuleCommandType.*;
import static com.cloudera.cyber.rules.RuleType.JS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
        verifySuccessfulResponse(null);


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
        verifyMessageScores(scoredMessages, scoredMessageTs, Collections.singletonList(Scores.builder().ruleId(ruleId).score(1.0).reason(expectedReason).build()));
        verifyMessageScores(scoredMessages, unscoredMessageTs, Collections.emptyList());
    }

    void verifyMessageScores(List<ScoredMessage> scoredMessages, long originalMessageTimestamp, List<Scores> expectedScores) {
        List<Scores> actualScores =  scoredMessages.stream().
                filter((m) -> (m.getTs() == originalMessageTimestamp)).
                map(ScoredMessage::getCyberScoresDetails).findFirst().orElse(Collections.emptyList());
        assertThat("message scores match", expectedScores, equalTo(actualScores));
    }

    private void verifySuccessfulResponse(ScoringRule expectedRule) throws TimeoutException {
        DynamicRuleCommandResult<ScoringRule> poll = queryResponse.poll();
        assertThat("Command succeed", poll.isSuccess());
        assertThat("rule matched", poll.getRule(), equalTo(expectedRule));
    }

    private void verifyListResult(List<ScoringRule> scoringRules) throws TimeoutException {
        DynamicRuleCommandResult<ScoringRule> actualScoringRule;
        for(ScoringRule expectedRule : scoringRules) {
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
        return source.getDataStream()
                .assignTimestampsAndWatermarks(new MessageBoundedOutOfOrder(Time.milliseconds(1000)))
                .setParallelism(1);
    }

    @Override
    protected DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params) {
        querySource = JobTester.createManualSource(env, TypeInformation.of(ScoringRuleCommand.class));

        return querySource.getDataStream()
                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<ScoringRuleCommand>(Time.milliseconds(1000)) {
                    @Override
                    public long extractTimestamp(ScoringRuleCommand scoringRuleCommand) {
                        return scoringRuleCommand.getTs();
                    }
                })
                .setParallelism(1);
    }

    @Override
    protected void writeQueryResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results) {
        results.addSink(queryResponse).name("Scoring command Results").setParallelism(1);
    }

}
