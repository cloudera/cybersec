package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.profiler.ProfileAggregationMethod;
import com.cloudera.cyber.profiler.ProfileGroupTest;
import com.cloudera.cyber.profiler.ProfileMeasurementConfig;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

public class ProfileGroupAccumulatorTest extends ProfileGroupTest {

    @Test
    public void testProfileGroupAddMessage() {
        ProfileGroupAccumulator profileGroupAccumulator = getProfileGroupAccumulator(PROFILE_GROUP_NAME);

        long currentTime = MessageUtils.getCurrentTimestamp();
        long[] valuesToSum = { 50, 25, 10};

        double expectedSum = valuesToSum[0];
        addMessage(profileGroupAccumulator, currentTime, valuesToSum[0]);
        verifyProfileGroupAccumulatorMessage(profileGroupAccumulator, currentTime, currentTime, expectedSum);

        long nextTime = currentTime + 100;
        expectedSum += valuesToSum[1];
        addMessage(profileGroupAccumulator, nextTime, valuesToSum[1]);
        verifyProfileGroupAccumulatorMessage(profileGroupAccumulator, currentTime, nextTime, expectedSum);

        long previousTime = currentTime - 100;
        expectedSum += valuesToSum[2];
        addMessage(profileGroupAccumulator, previousTime, valuesToSum[2]);
        verifyProfileGroupAccumulatorMessage(profileGroupAccumulator, previousTime, nextTime, expectedSum);
    }

    @Test
    public void testProfileGroupMerge() {
        ProfileGroupAccumulator acc1 = getProfileGroupAccumulator(PROFILE_GROUP_NAME);

        long currentTime = MessageUtils.getCurrentTimestamp();

        addMessage(acc1, currentTime, 50);
        verifyProfileGroupAccumulatorMessage(acc1, currentTime, currentTime, 50);

        ProfileGroupAccumulator acc2 = getProfileGroupAccumulator(PROFILE_GROUP_NAME);
        addMessage(acc2, currentTime - 100, 100);
        addMessage(acc2, currentTime + 100, 25);
        verifyProfileGroupAccumulatorMessage(acc2, currentTime - 100, currentTime + 100, 125);

        ProfileGroupAccumulator mergedAcc = acc1.merge(acc2);

        verifyProfileGroupAccumulatorMessage(mergedAcc, currentTime - 100, currentTime + 100, 175);
        verifyProfileGroupAccumulatorMessage(acc1, currentTime - 100, currentTime + 100, 175);
        verifyProfileGroupAccumulatorMessage(acc2, currentTime - 100, currentTime + 100, 125);
    }

    @Test
    public void testProfileGroupMergeIncompatibleOtherParameter() {
        ProfileGroupAccumulator acc1 = getProfileGroupAccumulator(PROFILE_GROUP_NAME);

        long currentTime = MessageUtils.getCurrentTimestamp();

        addMessage(acc1, currentTime, 50);

        // try to merge with itself
        acc1.merge(acc1);
        verifyProfileGroupAccumulatorMessage(acc1, currentTime, currentTime, 50);

        // merge with null
        acc1.merge(null);
        verifyProfileGroupAccumulatorMessage(acc1, currentTime, currentTime, 50);

        // merge with another profile group with a different name - value should stay same
        ProfileGroupAccumulator profileNameDoesntMatch = getProfileGroupAccumulator("different profile group");
        acc1.merge(profileNameDoesntMatch);
        verifyProfileGroupAccumulatorMessage(acc1, currentTime, currentTime, 50);

        // create a profile group with matching name but lists are different sizes
        ProfileMeasurementConfig sumConfig = ProfileMeasurementConfig.builder().resultExtensionName(RESULT_EXTENSION_NAME).
                aggregationMethod(ProfileAggregationMethod.SUM).fieldName(SUM_FIELD_NAME).format("0.000000").
                build();

        ProfileMeasurementConfig countConfig = ProfileMeasurementConfig.builder().resultExtensionName("count").
                aggregationMethod(ProfileAggregationMethod.COUNT).fieldName("count field").
                build();
        List<ProfileMeasurementConfig> differentMeasurementList = Lists.newArrayList(sumConfig, countConfig);
        ProfileGroupAccumulator differentMeasurementListSizes = getProfileGroupAccumulator(PROFILE_GROUP_NAME, differentMeasurementList);
        acc1.merge(differentMeasurementListSizes);
        verifyProfileGroupAccumulatorMessage(acc1, currentTime, currentTime, 50);
    }



}
