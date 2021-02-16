package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAccumulator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Assert;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ProfileGroupTest {
    protected static final String KEY_FIELD_NAME = "key_field";
    protected static final String KEY_FIELD_VALUE = "key_field_value";
    protected static final String PROFILE_GROUP_NAME = "test_profile";
    protected static final String RESULT_EXTENSION_NAME = "result";
    protected static final String SUM_FIELD_NAME = "field_to_sum";


    protected ProfileGroupAccumulator getProfileGroupAccumulator(String profileGroupName) {
        List<ProfileMeasurementConfig> measurements = Collections.singletonList(ProfileMeasurementConfig.builder().resultExtensionName(RESULT_EXTENSION_NAME).
                aggregationMethod(ProfileAggregationMethod.SUM).fieldName(SUM_FIELD_NAME).format("0.000000").
                build());
        return getProfileGroupAccumulator(profileGroupName, measurements);
    }

    protected ProfileGroupAccumulator getProfileGroupAccumulator(String profileGroupName, List<ProfileMeasurementConfig> measurements) {
        return new ProfileGroupAccumulator(getProfileGroupConfig(profileGroupName, measurements), false);
    }

    protected ProfileGroupConfig getProfileGroupConfig(String profileGroupName,  List<ProfileMeasurementConfig> measurements) {
        return ProfileGroupConfig.builder().profileGroupName(profileGroupName).keyFieldNames(Lists.newArrayList(KEY_FIELD_NAME))
                .sources(Lists.newArrayList("ANY")).
                        periodDuration(1L).periodDurationUnit(TimeUnit.MINUTES.name()).
                        measurements(Lists.newArrayList(measurements)).
                        build();
    }

    protected void addMessage(ProfileGroupAccumulator profileGroupAccumulator, long timestamp, long aggregationFieldValue) {
        profileGroupAccumulator.add(createMessage(timestamp, aggregationFieldValue));
    }

    protected Message createMessage(long timestamp, long aggregationFieldValue) {
        Map<String, String> extensions = ImmutableMap.of(
                KEY_FIELD_NAME, KEY_FIELD_VALUE,
                SUM_FIELD_NAME, Long.toString(aggregationFieldValue)
        );
        return TestUtils.createMessage(timestamp, "test", extensions);
    }

    protected void verifyProfileGroupAccumulatorMessage(ProfileGroupAccumulator profileGroupAccumulator, long startPeriod, long endPeriod, double profileResultValue) {
        Message profileMessage = profileGroupAccumulator.getProfileMessage();
        verifyProfileMessage(startPeriod, endPeriod, profileResultValue, profileMessage);
    }

    protected void verifyProfileMessage(long startPeriod, long endPeriod, double profileResultValue, Message profileMessage) {
        Map<String, String> extensions = profileMessage.getExtensions();
        Assert.assertEquals(endPeriod, profileMessage.getTs());
        Assert.assertEquals(PROFILE_GROUP_NAME, extensions.get(ProfileGroupAccumulator.PROFILE_GROUP_NAME_EXTENSION));
        Assert.assertEquals(KEY_FIELD_VALUE, extensions.get(KEY_FIELD_NAME));
        Assert.assertEquals(String.format("%f", profileResultValue), extensions.get(RESULT_EXTENSION_NAME));
        Assert.assertEquals(endPeriod, profileMessage.getTs());
        Assert.assertEquals(Long.toString(startPeriod), extensions.get(ProfileGroupAccumulator.START_PERIOD_EXTENSION));
        Assert.assertEquals(Long.toString(endPeriod), extensions.get(ProfileGroupAccumulator.END_PERIOD_EXTENSION));
        Assert.assertEquals(ProfileGroupAccumulator.PROFILE_SOURCE, profileMessage.getSource());
        Assert.assertEquals(ProfileGroupAccumulator.PROFILE_TOPIC_NAME, profileMessage.getOriginalSource().getTopic());
    }
}
