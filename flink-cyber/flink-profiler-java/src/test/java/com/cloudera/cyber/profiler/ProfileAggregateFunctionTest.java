package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAccumulator;
import org.junit.Assert;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloudera.cyber.profiler.accumulator.ProfileGroupAccumulator.*;
import static com.cloudera.cyber.profiler.accumulator.StatsProfileAccumulator.*;

public class ProfileAggregateFunctionTest extends ProfileAggregateTest {

    @Test
    public void testProfileAggregator() {
        ProfileGroupConfig profileGroupConfig = getProfileGroupConfig(false);
        ProfileAggregateFunction aggregateFunction = new ProfileAggregateFunction(profileGroupConfig, false);

        ProfileGroupAccumulator acc = aggregateFunction.createAccumulator();

        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        acc = verifyAddMessageResult(aggregateFunction, acc,
                currentTimestamp, 30,
                currentTimestamp, currentTimestamp, 30);

        acc = verifyAddMessageResult(aggregateFunction, acc,
                currentTimestamp + 100, 500,
                currentTimestamp, currentTimestamp + 100, 530);

        ProfileGroupAccumulator acc1 = aggregateFunction.createAccumulator();
        acc1 = verifyAddMessageResult(aggregateFunction, acc1,
                currentTimestamp - 100, 2000,
                 currentTimestamp - 100, currentTimestamp - 100, 2000);
        ProfileGroupAccumulator mergeAcc = aggregateFunction.merge(acc, acc1);
        Message profileMessage = aggregateFunction.getResult(mergeAcc);
        verifyProfileMessage(currentTimestamp - 100, currentTimestamp + 100, 2530, profileMessage);
    }

    @Test
    public void testProfileAggregatorWithStats() {
        ProfileGroupConfig profileGroupConfig = getProfileGroupConfig(true);

        // aggregate a profile
        ProfileAggregateFunction aggregateFunction = new ProfileAggregateFunction(profileGroupConfig, false);
        ProfileGroupAccumulator acc = aggregateFunction.createAccumulator();

        ProfileAggregateFunction statsFunction = new ProfileAggregateFunction(profileGroupConfig, true);
        ProfileGroupAccumulator statsAcc = statsFunction.createAccumulator();

        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        acc = verifyAddMessageResult(aggregateFunction, acc,
                currentTimestamp, 30,
                currentTimestamp, currentTimestamp, 30);
        statsAcc = statsFunction.add(acc.getProfileMessage(), statsAcc);

        acc = verifyAddMessageResult(aggregateFunction, acc,
                currentTimestamp + 100, 500,
                currentTimestamp, currentTimestamp + 100, 530);
        statsAcc = statsFunction.add(acc.getProfileMessage(), statsAcc);

        verifyStatsMessage(statsFunction.getResult(statsAcc), new DecimalFormat(profileGroupConfig.getMeasurements().get(0).getFormat()),
                currentTimestamp, currentTimestamp + 100,
                30, 530, 280, 353.553391);
    }

    private void verifyStatsMessage(Message message, DecimalFormat format, long startPeriod, long endPeriod, double expectedMin, double expectedMax, double expectedMean, double expectedStdDev) {
        Map<String, String> expectedExtensions = new HashMap<String, String>() {{
            put(PROFILE_GROUP_NAME_EXTENSION, PROFILE_GROUP_NAME.concat(".stats"));
            put(START_PERIOD_EXTENSION, Long.toString(startPeriod));
            put(END_PERIOD_EXTENSION, Long.toString(endPeriod));
            put(RESULT_EXTENSION_NAME.concat(MIN_RESULT_SUFFIX), format.format(expectedMin));
            put(RESULT_EXTENSION_NAME.concat(MAX_RESULT_SUFFIX), format.format(expectedMax));
            put(RESULT_EXTENSION_NAME.concat(MEAN_RESULT_SUFFIX), format.format(expectedMean));
            put(RESULT_EXTENSION_NAME.concat(STDDEV_RESULT_SUFFIX), format.format(expectedStdDev));
        }};
        Assert.assertEquals(expectedExtensions, message.getExtensions());
    }
    private ProfileGroupAccumulator verifyAddMessageResult(ProfileAggregateFunction aggregateFunction, ProfileGroupAccumulator acc,
                                          long timestamp, long aggregateFieldValue,
                                          long expectedStartPeriod, long expectedEndPeriod, double expectedProfileResult) {
        Message firstMessage = createMessage(timestamp, aggregateFieldValue);
        ProfileGroupAccumulator acc1 = aggregateFunction.add(firstMessage, acc);
        Message profileMessage = aggregateFunction.getResult(acc1);
        verifyProfileMessage(expectedStartPeriod, expectedEndPeriod, expectedProfileResult, profileMessage);

        return acc1;
    }

}
