package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.profiler.accumulator.FieldValueProfileGroupAccTest;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import org.junit.Assert;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.Map;

import static com.cloudera.cyber.profiler.ProfileAggregateFunction.*;
import static com.cloudera.cyber.profiler.accumulator.FieldValueProfileGroupAccTest.*;
import static com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc.END_PERIOD_EXTENSION;
import static com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc.START_PERIOD_EXTENSION;

public class FieldProfileAggregateFunction {

    @Test
    public void testProfileAggregator() {
        ProfileGroupConfig profileGroupConfig = FieldValueProfileGroupAccTest.createProfileGroupConfig(null);
        ProfileAggregateFunction aggregateFunction = new FieldValueProfileAggregateFunction(profileGroupConfig);

        ProfileGroupAcc acc = aggregateFunction.createAccumulator();

        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        Message profileMessage = getProfileMessage(aggregateFunction, acc,
                currentTimestamp,500, "1 string", 10000, 8);
        verifyProfileMessage(profileGroupConfig, profileMessage,  currentTimestamp, currentTimestamp, KEY_1_VALUE, KEY_2_VALUE,
                500, 1, 1, 10000, 8, currentTimestamp);

        profileMessage = getProfileMessage(aggregateFunction, acc,
                currentTimestamp - 1, 10, "2 string", 10100, 2);
        verifyProfileMessage(profileGroupConfig, profileMessage,  currentTimestamp - 1, currentTimestamp, KEY_1_VALUE, KEY_2_VALUE,
                510, 2, 2, 10100, 2, currentTimestamp - 1);


        ProfileGroupAcc acc1 = aggregateFunction.createAccumulator();
        profileMessage = getProfileMessage(aggregateFunction, acc1,
                currentTimestamp + 5,50000, "3 string", 100000, 1);
        verifyProfileMessage(profileGroupConfig, profileMessage,  currentTimestamp + 5, currentTimestamp + 5, KEY_1_VALUE, KEY_2_VALUE,
                50000, 1, 1, 100000, 1, currentTimestamp + 5);

        ProfileGroupAcc mergeAcc = aggregateFunction.merge(acc, acc1);
        profileMessage = aggregateFunction.getResult(mergeAcc);
        verifyProfileMessage(profileGroupConfig, profileMessage,  currentTimestamp - 1, currentTimestamp + 5, KEY_1_VALUE, KEY_2_VALUE,
                50510, 3, 3, 100000, 1, currentTimestamp - 1);
    }

    private void verifyProfileMessage(ProfileGroupConfig profileGroupConfig, Message profileMessage, long startPeriod, long endPeriod, String key1, String key2, double sum, double count,
                               double countDistinct, double max, double min, long firstSeen) {

        Assert.assertEquals(PROFILE_TOPIC_NAME, profileMessage.getOriginalSource().getTopic());
        Assert.assertEquals(PROFILE_SOURCE, profileMessage.getSource());
        Assert.assertEquals(endPeriod, profileMessage.getTs());

        Map<String, DecimalFormat> formats = getFormats(profileGroupConfig);
        Map<String, String> actualExtensions = profileMessage.getExtensions();
        Assert.assertEquals(profileGroupConfig.getProfileGroupName(), actualExtensions.get(PROFILE_GROUP_NAME_EXTENSION));
        Assert.assertEquals(Long.toString(startPeriod), actualExtensions.get(START_PERIOD_EXTENSION));
        Assert.assertEquals(Long.toString(endPeriod), actualExtensions.get(END_PERIOD_EXTENSION));
        Assert.assertEquals(formats.get(SUM_RESULT).format(sum), actualExtensions.get(SUM_RESULT));
        Assert.assertEquals(formats.get(COUNT_RESULT).format(count), actualExtensions.get(COUNT_RESULT));
        Assert.assertEquals(formats.get(COUNT_DIST_RESULT).format(countDistinct), actualExtensions.get(COUNT_DIST_RESULT));
        Assert.assertEquals(formats.get(MAX_RESULT).format(max), actualExtensions.get(MAX_RESULT));
        Assert.assertEquals(formats.get(MIN_RESULT).format(min), actualExtensions.get(MIN_RESULT));
        Assert.assertEquals(formats.get(FIRST_SEEN_RESULT).format(firstSeen), actualExtensions.get(FIRST_SEEN_RESULT));
        if (key1 != null && key2 != null) {
            Assert.assertEquals(key1, actualExtensions.get(KEY_1));
            Assert.assertEquals(key2, actualExtensions.get(KEY_2));
            Assert.assertEquals(11, actualExtensions.size());
        } else {
            Assert.assertEquals(9, actualExtensions.size());
        }
    }

    private Message getProfileMessage(ProfileAggregateFunction aggregateFunction, ProfileGroupAcc acc,
                                      long timestamp, double sum,
                                      String countDistinct, double max, double min) {
        Message firstMessage = createMessage(timestamp, sum, countDistinct, max, min);
        ProfileGroupAcc acc1 = aggregateFunction.add(firstMessage, acc);
        return aggregateFunction.getResult(acc1);
    }
}
