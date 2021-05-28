package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.profiler.ProfileAggregationMethod;
import com.cloudera.cyber.profiler.ProfileGroupConfig;
import com.cloudera.cyber.profiler.ProfileMeasurementConfig;
import com.cloudera.cyber.profiler.ProfileMessage;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc.END_PERIOD_EXTENSION;
import static com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc.START_PERIOD_EXTENSION;

public class FieldValueProfileGroupAccTest extends ProfileGroupConfigTestUtils {
    public static final String SUM_FIELD = "sum_field";
    public static final String COUNT_DIST_FIELD = "count_dist_field";
    public static final String MAX_FIELD = "max_field";
    public static final String MIN_FIELD = "min_field";
    public static final String SUM_RESULT = "sum_result";
    public static final String COUNT_RESULT = "count_result";
    public static final String COUNT_DIST_RESULT = "count_dist_result";
    public static final String MAX_RESULT = "max_result";
    public static final String MIN_RESULT = "min_result";
    public static final String FIRST_SEEN_RESULT = "first_seen_result";


    @Test
    public void testDefaultFormat() {
        testValueGroupAccumulator(null);
    }

    @Test
    public void testCustomFormat() {
        testValueGroupAccumulator("0.00");
    }

    @Test
    public void testMerge() {
        ArrayList<ProfileMeasurementConfig> measurements = Lists.newArrayList(
                createMeasurement(ProfileAggregationMethod.SUM, SUM_RESULT, SUM_FIELD),
                createMeasurement(ProfileAggregationMethod.COUNT, COUNT_RESULT, null),
                createMeasurement(ProfileAggregationMethod.COUNT_DISTINCT, COUNT_DIST_RESULT, COUNT_DIST_FIELD),
                createMeasurement(ProfileAggregationMethod.MAX, MAX_RESULT, MAX_FIELD),
                createMeasurement(ProfileAggregationMethod.MIN, MIN_RESULT, MIN_FIELD),
                createMeasurement(ProfileAggregationMethod.FIRST_SEEN, FIRST_SEEN_RESULT, null));
        ProfileGroupConfig profileGroupConfig = ProfileGroupConfig.builder().
                profileGroupName(TEST_PROFILE_GROUP).keyFieldNames(Lists.newArrayList(KEY_1, KEY_2)).
                periodDuration(5L).periodDurationUnit("MINUTES").
                sources(Lists.newArrayList("ANY")).measurements(measurements).build();

        // create the first accumulator
        FieldValueProfileGroupAcc acc1 = new FieldValueProfileGroupAcc(profileGroupConfig);
        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        acc1.addMessage(createMessage(currentTimestamp, 5, "first string",60, 10), profileGroupConfig);
        verifyResults(profileGroupConfig, acc1, currentTimestamp, currentTimestamp, KEY_1_VALUE, KEY_2_VALUE,
                5, 1, 1, 60, 10);

        // create the second accumulator
        FieldValueProfileGroupAcc acc2 = new FieldValueProfileGroupAcc(profileGroupConfig);
        acc2.addMessage(createMessage(currentTimestamp - 1, 10, "second string", 1000, 4), profileGroupConfig);
        verifyResults(profileGroupConfig, acc2, currentTimestamp - 1, currentTimestamp - 1, KEY_1_VALUE, KEY_2_VALUE,
                10, 1, 1, 1000, 4);

        acc2.addMessage(createMessage(currentTimestamp + 1, 30, "first string", 500, 300), profileGroupConfig);
        verifyResults(profileGroupConfig, acc2, currentTimestamp - 1, currentTimestamp + 1, KEY_1_VALUE, KEY_2_VALUE,
                40, 2, 2, 1000, 4);

        // merge together
        acc1.merge(acc2);
        verifyResults(profileGroupConfig, acc1, currentTimestamp - 1, currentTimestamp + 1, KEY_1_VALUE, KEY_2_VALUE,
                45,  3, 2, 1000, 4);

        // merge into an empty accumulator
        FieldValueProfileGroupAcc emptyAcc = new FieldValueProfileGroupAcc(profileGroupConfig);
        emptyAcc.merge(acc2);
        verifyResults(profileGroupConfig, acc1, currentTimestamp - 1, currentTimestamp + 1, KEY_1_VALUE, KEY_2_VALUE,
                45,  3, 2, 1000, 4);
    }

    private void testValueGroupAccumulator(String format) {
        ProfileGroupConfig profileGroupConfig = createProfileGroupConfig(format);

        FieldValueProfileGroupAcc valueAcc = new FieldValueProfileGroupAcc(profileGroupConfig);
        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        double expectedSum = 0;
        double expectedCount= 0;
        double expectedCountDistinct = 0;
        double expectedMax = Double.NEGATIVE_INFINITY;
        double expectedMin = Double.POSITIVE_INFINITY;

        // check results of default values
        verifyResults(profileGroupConfig, valueAcc, Long.MAX_VALUE, Long.MIN_VALUE, null, null,
                expectedSum, expectedCount, expectedCountDistinct, expectedMax, expectedMin);

        // add a message with all fields set
        valueAcc.addMessage(createMessage(currentTimestamp, 5, "first string",60, 10), profileGroupConfig);
        expectedSum += 5;
        expectedCount += 1;
        expectedCountDistinct = 1;
        expectedMax = 60;
        expectedMin = 10;
        verifyResults(profileGroupConfig, valueAcc, currentTimestamp, currentTimestamp, KEY_1_VALUE, KEY_2_VALUE,
                expectedSum, expectedCount, expectedCountDistinct, expectedMax, expectedMin);

        // add a message with no extensions - should only update count and first seen since timestamp is earlier
        expectedCount += 1;
        valueAcc.addMessage(new ProfileMessage(currentTimestamp - 1,  Collections.emptyMap()), profileGroupConfig);
        verifyResults(profileGroupConfig, valueAcc, currentTimestamp - 1, currentTimestamp, KEY_1_VALUE, KEY_2_VALUE,
                expectedSum, expectedCount, expectedCountDistinct, expectedMax, expectedMin);

        // add a message with invalid numbers encoded as strings
        valueAcc.addMessage(createInvalidDoubleNumberMessage(currentTimestamp + 1), profileGroupConfig);
        expectedCount += 1;
        expectedCountDistinct += 1;
        verifyResults(profileGroupConfig, valueAcc, currentTimestamp - 1, currentTimestamp + 1, KEY_1_VALUE, KEY_2_VALUE,
                expectedSum, expectedCount, expectedCountDistinct, expectedMax, expectedMin);

        // add another valid message
        valueAcc.addMessage(createMessage(currentTimestamp + 3, 1000, "a third string",60000, 4), profileGroupConfig);
        expectedSum += 1000;
        expectedCount += 1;
        expectedCountDistinct = 3;
        expectedMax = 60000;
        expectedMin = 4;
        verifyResults(profileGroupConfig, valueAcc, currentTimestamp - 1, currentTimestamp + 3, KEY_1_VALUE, KEY_2_VALUE,
                expectedSum, expectedCount, expectedCountDistinct, expectedMax, expectedMin);

    }

    public static ProfileGroupConfig createProfileGroupConfig(String format) {
        ArrayList<ProfileMeasurementConfig> measurements = Lists.newArrayList(
                createMeasurement(ProfileAggregationMethod.SUM, SUM_RESULT, SUM_FIELD, format),
                createMeasurement(ProfileAggregationMethod.COUNT, COUNT_RESULT, null, format),
                createMeasurement(ProfileAggregationMethod.COUNT_DISTINCT, COUNT_DIST_RESULT, COUNT_DIST_FIELD, format),
                createMeasurement(ProfileAggregationMethod.MAX, MAX_RESULT, MAX_FIELD, format),
                createMeasurement(ProfileAggregationMethod.MIN, MIN_RESULT, MIN_FIELD, format),
                createMeasurement(ProfileAggregationMethod.FIRST_SEEN, FIRST_SEEN_RESULT, null, format));
        return ProfileGroupConfig.builder().
                profileGroupName(TEST_PROFILE_GROUP).keyFieldNames(Lists.newArrayList(KEY_1, KEY_2)).
                periodDuration(5L).periodDurationUnit("MINUTES").
                sources(Lists.newArrayList("ANY")).measurements(measurements).build();

    }

    private ProfileMessage createInvalidDoubleNumberMessage(long timestamp) {
        Map<String, String> extensions = new HashMap<String, String>() {{
            put(KEY_1, KEY_1_VALUE);
            put(KEY_2, KEY_2_VALUE);
            put(SUM_FIELD, "Bad number 1");
            put(COUNT_DIST_FIELD, "distinct string");
            put(MAX_FIELD, "Bad number 2");
            put(MIN_FIELD, "Bad number 3");
        }};
        return new ProfileMessage(timestamp, extensions);
    }


    public static ProfileMessage createMessage(long timestamp, double sum_field, String count_dist_field,
                                               double max_field, double min_field) {
        Map<String, String> extensions = new HashMap<String, String>() {{
                put(KEY_1, KEY_1_VALUE);
                put(KEY_2, KEY_2_VALUE);
                put(SUM_FIELD, Double.toString(sum_field));
                put(COUNT_DIST_FIELD, count_dist_field);
                put(MAX_FIELD, Double.toString(max_field));
                put(MIN_FIELD, Double.toString(min_field));
        }};
        return new ProfileMessage(timestamp, extensions);
    }

    public static Map<String, DecimalFormat> getFormats(ProfileGroupConfig profileGroupConfig){
        return profileGroupConfig.getMeasurements().stream().
                collect(Collectors.toMap(ProfileMeasurementConfig::getResultExtensionName,
                        ProfileMeasurementConfig::getDecimalFormat));
    }

    private void verifyResults(ProfileGroupConfig profileGroupConfig, FieldValueProfileGroupAcc acc, long startPeriod, long endPeriod, String key1, String key2, double sum, double count,
                               double countDistinct, double max, double min) {

        Assert.assertEquals(endPeriod, acc.getEndTimestamp());
        Map<String, DecimalFormat> formats = getFormats(profileGroupConfig);
        Map<String, String> actualExtensions = acc.getProfileExtensions(profileGroupConfig, formats);
        Assert.assertEquals(Long.toString(startPeriod), actualExtensions.get(START_PERIOD_EXTENSION));
        Assert.assertEquals(Long.toString(endPeriod), actualExtensions.get(END_PERIOD_EXTENSION));
        Assert.assertEquals(formats.get(SUM_RESULT).format(sum), actualExtensions.get(SUM_RESULT));
        Assert.assertEquals(formats.get(COUNT_RESULT).format(count), actualExtensions.get(COUNT_RESULT));
        Assert.assertEquals(formats.get(COUNT_DIST_RESULT).format(countDistinct), actualExtensions.get(COUNT_DIST_RESULT));
        Assert.assertEquals(formats.get(MAX_RESULT).format(max), actualExtensions.get(MAX_RESULT));
        Assert.assertEquals(formats.get(MIN_RESULT).format(min), actualExtensions.get(MIN_RESULT));
        if (key1 != null && key2 != null) {
            Assert.assertEquals(key1, actualExtensions.get(KEY_1));
            Assert.assertEquals(key2, actualExtensions.get(KEY_2));
            Assert.assertEquals(9, actualExtensions.size());
        } else {
            Assert.assertEquals(7, actualExtensions.size());
        }
    }

}
