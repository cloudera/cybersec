package com.cloudera.cyber.profiler;

import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.profiler.accumulator.FieldValueProfileGroupAccTest;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import org.junit.Assert;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.Map;

import static com.cloudera.cyber.profiler.ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION;
import static com.cloudera.cyber.profiler.accumulator.FieldValueProfileGroupAccTest.*;
import static com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc.END_PERIOD_EXTENSION;
import static com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc.START_PERIOD_EXTENSION;

public class FieldValueProfileAggregateFunctionTest {

    @Test
    public void testProfileAggregator() {
        ProfileGroupConfig profileGroupConfig = FieldValueProfileGroupAccTest.createProfileGroupConfig(null);
        ProfileAggregateFunction aggregateFunction = new FieldValueProfileAggregateFunction(profileGroupConfig);

        ProfileGroupAcc acc = aggregateFunction.createAccumulator();

        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        ProfileMessage profileMessage = getProfileMessage(aggregateFunction, acc,
                currentTimestamp,500, "1 string", 10000, 8);
        verifyProfileMessage(profileGroupConfig, profileMessage,  currentTimestamp, currentTimestamp,
                500, 1, 1, 10000, 8);

        profileMessage = getProfileMessage(aggregateFunction, acc,
                currentTimestamp - 1, 10, "2 string", 10100, 2);
        verifyProfileMessage(profileGroupConfig, profileMessage,  currentTimestamp - 1, currentTimestamp,
                510, 2, 2, 10100, 2);


        ProfileGroupAcc acc1 = aggregateFunction.createAccumulator();
        profileMessage = getProfileMessage(aggregateFunction, acc1,
                currentTimestamp + 5,50000, "3 string", 100000, 1);
        verifyProfileMessage(profileGroupConfig, profileMessage,  currentTimestamp + 5, currentTimestamp + 5,
                50000, 1, 1, 100000, 1);

        ProfileGroupAcc mergeAcc = aggregateFunction.merge(acc, acc1);
        profileMessage = aggregateFunction.getResult(mergeAcc);
        verifyProfileMessage(profileGroupConfig, profileMessage,  currentTimestamp - 1, currentTimestamp + 5,
                50510, 3, 3, 100000, 1);
    }

    private void verifyProfileMessage(ProfileGroupConfig profileGroupConfig, ProfileMessage profileMessage, long startPeriod, long endPeriod, double sum, double count,
                                      double countDistinct, double max, double min) {

        Assert.assertEquals(endPeriod, profileMessage.getTs());

        Map<String, DecimalFormat> formats = getFormats(profileGroupConfig);
        Map<String, String> actualExtensions = profileMessage.getExtensions();
        Assert.assertEquals(profileGroupConfig.getProfileGroupName(), actualExtensions.get(PROFILE_GROUP_NAME_EXTENSION));
        Assert.assertEquals(Long.toString(startPeriod), actualExtensions.get(START_PERIOD_EXTENSION));
        Assert.assertEquals(formats.get(MIN_RESULT).format(min), actualExtensions.get(MIN_RESULT));
        Assert.assertEquals(formats.get(SUM_RESULT).format(sum), actualExtensions.get(SUM_RESULT));
        Assert.assertEquals(Long.toString(endPeriod), actualExtensions.get(END_PERIOD_EXTENSION));
        Assert.assertEquals(formats.get(MAX_RESULT).format(max), actualExtensions.get(MAX_RESULT));
        Assert.assertEquals(com.cloudera.cyber.profiler.accumulator.ProfileGroupConfigTestUtils.KEY_1_VALUE, actualExtensions.get(KEY_1));
        Assert.assertEquals(com.cloudera.cyber.profiler.accumulator.ProfileGroupConfigTestUtils.KEY_2_VALUE, actualExtensions.get(KEY_2));
        Assert.assertEquals(formats.get(COUNT_RESULT).format(count), actualExtensions.get(COUNT_RESULT));
        Assert.assertEquals(formats.get(COUNT_DIST_RESULT).format(countDistinct), actualExtensions.get(COUNT_DIST_RESULT));
        Assert.assertEquals(10, actualExtensions.size());
    }

    private ProfileMessage getProfileMessage(ProfileAggregateFunction aggregateFunction, ProfileGroupAcc acc,
                                      long timestamp, double sum,
                                      String countDistinct, double max, double min) {
        ProfileMessage firstMessage = createMessage(timestamp, sum, countDistinct, max, min);
        ProfileGroupAcc acc1 = aggregateFunction.add(firstMessage, acc);
        return aggregateFunction.getResult(acc1);
    }
}
