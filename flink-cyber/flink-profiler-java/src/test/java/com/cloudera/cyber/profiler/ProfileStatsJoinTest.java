package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAccumulator;
import com.cloudera.cyber.profiler.accumulator.StatsProfileAccumulator;
import com.fasterxml.jackson.core.exc.StreamReadException;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.TestHarnessUtil;
import org.apache.flink.streaming.util.TwoInputStreamOperatorTestHarness;
import org.junit.Test;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;


public class ProfileStatsJoinTest extends ProfileAggregateTest {

    @Test
    public void profileJoinTestWithStats() throws Exception {
        testJoinProfiles(true);
    }

    @Test
    public void profileJoinTestBeforeStats() throws Exception {
        testJoinProfiles(false);
    }

    private void testJoinProfiles(boolean statsFirst) throws Exception {
        ProfileStatsJoin join = new ProfileStatsJoin();
        join.open(new Configuration());

        ProfileGroupConfig profileGroupConfig = getProfileGroupConfig(true);
        Message profileMessage = createProfileMessage(profileGroupConfig);
        Message statsMessage = createStatsMessage(profileGroupConfig, profileMessage);

        TwoInputStreamOperatorTestHarness<Message, Message, Message> harness = ProcessFunctionTestHarnesses.forCoProcessFunction(join);
        harness.setup();
        harness.open();
        ConcurrentLinkedQueue<Object> expectedOutput = new ConcurrentLinkedQueue<>();

        if (statsFirst) {
            sendMessage2(harness, statsMessage, expectedOutput);
            sendMessage1(harness, profileMessage, statsMessage, expectedOutput);
        } else {
            sendMessage1(harness, profileMessage, null, expectedOutput);
            sendMessage2(harness, statsMessage, expectedOutput);
        }
        Watermark watermark = new Watermark(profileMessage.getTs() + 200);
        expectedOutput.add(watermark);
        harness.processBothWatermarks(watermark);

        TestHarnessUtil.assertOutputEquals("output events equal", expectedOutput, harness.getOutput());
        harness.close();
    }

    private void sendMessage1(TwoInputStreamOperatorTestHarness<Message, Message, Message> harness, Message message1, Message message2, ConcurrentLinkedQueue<Object> expectedOutput) throws Exception {
        Message resultMessage = message1;
        if (message2 != null) {
            resultMessage = MessageUtils.addFields(message1, message2.getExtensions().entrySet().stream().
                    filter(e -> StatsProfileAccumulator.isStatsExtension(e.getKey())).
                    collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
        StreamRecord<Message> record = new StreamRecord<>(message1);
        harness.processElement1(record);
        expectedOutput.add(new StreamRecord<>(resultMessage));
    }

    private void sendMessage2(TwoInputStreamOperatorTestHarness<Message, Message, Message> harness, Message message2, ConcurrentLinkedQueue<Object> expectedOutput) throws Exception {
        harness.processElement2(new StreamRecord<>(message2));
        expectedOutput.add(new StreamRecord<>(message2));
    }

    private Message createProfileMessage(ProfileGroupConfig profileGroupConfig) {
        ProfileAggregateFunction aggregateFunction = new ProfileAggregateFunction(profileGroupConfig, false);

        ProfileGroupAccumulator acc = aggregateFunction.createAccumulator();
        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        addMessage(acc, currentTimestamp, 30);
        addMessage(acc, currentTimestamp + 100, 100);

        return acc.getProfileMessage();
    }

    private Message createStatsMessage(ProfileGroupConfig profileGroupConfig, Message profileMessage) {
        ProfileAggregateFunction aggregateFunction = new ProfileAggregateFunction(profileGroupConfig, true);
        ProfileGroupAccumulator acc = aggregateFunction.createAccumulator();
        acc.add(profileMessage);

        return acc.getProfileMessage();
    }
}
