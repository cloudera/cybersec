package com.cloudera.cyber.scoring;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.MessageBoundedOutOfOrder;
import com.cloudera.cyber.rules.DynamicRuleCommandResult;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.cloudera.cyber.rules.DynamicRuleCommandType.*;
import static com.cloudera.cyber.rules.RuleType.JS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestScoringJob extends ScoringJob {

    private ManualSource<Message> source;
    private ManualSource<ScoringRuleCommand> querySource;

    private CollectingSink<ScoredMessage> sink = new CollectingSink<>();
    private CollectingSink<DynamicRuleCommandResult<ScoringRule>> queryResponse = new CollectingSink<>();

    private List<Message> recordLog = new ArrayList<>();
    private List<ScoringRuleCommand> ruleLog = new ArrayList<>();


    @Test
    public void testPipeline() throws Exception {
        StreamExecutionEnvironment env = createPipeline(ParameterTool.fromMap(Collections.emptyMap()));
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.enableCheckpointing(100l);
        JobTester.startTest(env);

        UUID ruleId = UUID.randomUUID();

        source.sendWatermark(0l);
        querySource.sendWatermark(0l);

        sendRule(ScoringRuleCommand.builder()
                .type(UPSERT)
                .rule(ScoringRule.builder()
                        .id(ruleId)
                        .name("test-rule")
                        .order(0)
                        .type(JS)
                        .ruleScript("return { score: 1.0, reason: message.test }")
                        .enabled(true)
                        .build())
                .ts(1L)
                .id(UUID.randomUUID())
                .build());

        DynamicRuleCommandResult<ScoringRule> poll = queryResponse.poll();
        assertThat("Command succeed", poll.isSuccess());

        // send a message and get score
        sendRecord(Message.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTs(org.joda.time.Instant.ofEpochMilli(100l).toDateTime())
                .setExtensions(Collections.singletonMap("test", "test-value"))
                .setOriginalSource("test-value").build());

        source.sendWatermark(100l);
        querySource.sendWatermark(100l);

        ScoredMessage message = sink.poll();
        assertThat("message got scored", message.getScores(), hasSize(1));

        sendRule(ScoringRuleCommand.builder()
                .type(LIST).ts(900).id(UUID.randomUUID()).build());

        DynamicRuleCommandResult<ScoringRule> poll1 = queryResponse.poll(Duration.ofMillis(1000));
        assertThat(poll1.getRule(), hasProperty("name", equalTo("test-rule")));


        sendRule(ScoringRuleCommand.builder()
                .type(DELETE)
                .ts(1000l)
                .id(UUID.randomUUID())
                .ruleId(ruleId).build());

        DynamicRuleCommandResult<ScoringRule> poll2 = queryResponse.poll(Duration.ofMillis(1000));

        Thread.sleep(100);

        sendRecord(Message.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setExtensions(Collections.singletonMap("test", "test-value2"))
                .setTs(org.joda.time.Instant.ofEpochMilli(2000l).toDateTime())
                .setOriginalSource("test-value2").build());

        ScoredMessage message1 = sink.poll(Duration.ofMillis(5000));
        assertThat("message got scored", message1.getScores(), nullValue());

        JobTester.stopTest();

    }

    private void sendRecord(Message d) {
        this.source.sendRecord(d, d.getTs().getMillis());
        this.recordLog.add(d);
    }

    private void sendRule(ScoringRuleCommand c) {
        this.querySource.sendRecord(c, c.getTs());
        this.ruleLog.add(c);
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<ScoredMessage> results) {
        results.addSink(sink);
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
    protected void writeQueryResult(ParameterTool params, DataStream<DynamicRuleCommandResult<ScoringRule>> results) {
        results.addSink(queryResponse);
    }
}
