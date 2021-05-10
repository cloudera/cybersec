package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.profiler.ProfileAggregationMethod;
import com.cloudera.cyber.profiler.ProfileGroupConfig;
import com.cloudera.cyber.profiler.ProfileMeasurementConfig;
import com.cloudera.cyber.profiler.ProfileMessage;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloudera.cyber.profiler.accumulator.ProfileGroupConfigTestUtils.*;


public class StatsProfileGroupAccTest {
    public static final String NO_STATS_RESULT_NAME = "no_stats_result";
    public static final String STATS_FIELD_NAME = "stats";
    public static final String STATS_RESULT_NAME = "stats_result";
    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("0.00");

    private Map<String, DecimalFormat> getMeasurementFormats(ProfileGroupConfig profileGroupConfig) {
        return profileGroupConfig.getMeasurements().stream().filter(ProfileMeasurementConfig::hasStats).
                collect(Collectors.toMap(ProfileMeasurementConfig::getResultExtensionName, v -> DEFAULT_FORMAT));
    }

    @Test
    public void testStatsAggregator() {

        ProfileGroupConfig profileGroupConfig = createProfileGroup();
        Map<String, DecimalFormat> formats = getMeasurementFormats(profileGroupConfig);

        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        StatsProfileGroupAcc acc = new StatsProfileGroupAcc(profileGroupConfig);
        testAddMessage(acc, profileGroupConfig, currentTimestamp, currentTimestamp, currentTimestamp, 50, 1, formats, 50, 50, 50, 0);
        testAddMessage(acc, profileGroupConfig,   currentTimestamp - 1, currentTimestamp - 1, currentTimestamp, 25, 2, formats, 25, 50, 37.5, 17.68);
        testAddMessage(acc, profileGroupConfig,   currentTimestamp + 1, currentTimestamp - 1, currentTimestamp + 1, 100, 3, formats, 25, 100, 58.33, 38.19);
        testAddMessage(acc, profileGroupConfig, currentTimestamp, currentTimestamp - 1, currentTimestamp + 1,"not a num", "4", formats, 25, 100, 58.33, 38.19);
    }

    @Test
    public void testStatsAggregatorMerge() {
        ProfileGroupConfig profileGroupConfig = createProfileGroup();
        Map<String, DecimalFormat> formats = getMeasurementFormats(profileGroupConfig);
        StatsProfileGroupAcc acc1 = new StatsProfileGroupAcc(profileGroupConfig);

        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        testAddMessage(acc1, profileGroupConfig, currentTimestamp, currentTimestamp, currentTimestamp, 50, 1, formats, 50, 50, 50, 0);
        testAddMessage(acc1, profileGroupConfig, currentTimestamp, currentTimestamp, currentTimestamp,25, 2, formats, 25, 50, 37.5, 17.68);

        StatsProfileGroupAcc acc2 = new StatsProfileGroupAcc(profileGroupConfig);
        testAddMessage(acc2, profileGroupConfig, currentTimestamp - 5, currentTimestamp - 5, currentTimestamp - 5,1000, 3, formats, 1000, 1000, 1000, 0);
        testAddMessage(acc2, profileGroupConfig, currentTimestamp + 5, currentTimestamp - 5, currentTimestamp + 5,5, 4, formats, 5, 1000, 502.5, 703.57);

        acc1.merge(acc2);
        verifyStatsResult(acc1, profileGroupConfig, formats,currentTimestamp - 5, currentTimestamp + 5, 5, 1000, 270, 487.01);
    }

    public static ProfileGroupConfig createProfileGroup() {
        ArrayList<ProfileMeasurementConfig> measurements = Lists.newArrayList(
                createMeasurement(ProfileAggregationMethod.COUNT, NO_STATS_RESULT_NAME, null, "0.00", false),
                createMeasurement(ProfileAggregationMethod.SUM, STATS_RESULT_NAME, STATS_FIELD_NAME, "0.00", true));

        return ProfileGroupConfig.builder().
                profileGroupName(TEST_PROFILE_GROUP).keyFieldNames(Lists.newArrayList(KEY_1, KEY_2)).
                periodDuration(5L).periodDurationUnit("MINUTES").
                sources(Lists.newArrayList("ANY")).measurements(measurements).build();

    }

    private void testAddMessage(StatsProfileGroupAcc acc, ProfileGroupConfig profileGroupConfig, long timestamp,  long startPeriod, long endPeriod,
                                double statsFieldValue, double noStatsFieldValue, Map<String, DecimalFormat> formats,
                                double expectedMin, double expectedMax, double expectedMean, double expectedStdDev) {
        testAddMessage(acc, profileGroupConfig, timestamp, startPeriod, endPeriod, DEFAULT_FORMAT.format(statsFieldValue), DEFAULT_FORMAT.format(noStatsFieldValue), formats,
                expectedMin, expectedMax, expectedMean, expectedStdDev);
    }

    private void testAddMessage(StatsProfileGroupAcc acc, ProfileGroupConfig profileGroupConfig, long timestamp, long startPeriod, long endPeriod,
                                String statsFieldValue, String noStatsFieldValue, Map<String, DecimalFormat> formats,
                                double expectedMin, double expectedMax, double expectedMean, double expectedStdDev) {
        acc.addMessage(new ProfileMessage(timestamp, ImmutableMap.of(STATS_RESULT_NAME, statsFieldValue, NO_STATS_RESULT_NAME, noStatsFieldValue)), profileGroupConfig);
        verifyStatsResult(acc, profileGroupConfig, formats, startPeriod, endPeriod, expectedMin, expectedMax, expectedMean, expectedStdDev);
    }

    private static void verifyStatsResult(StatsProfileGroupAcc acc, ProfileGroupConfig profileGroupConfig, Map<String, DecimalFormat> formats,
                                          long startPeriod, long endPeriod, double expectedMin, double expectedMax, double expectedMean, double expectedStdDev) {
        Map<String, String> actualExtensions = acc.getProfileExtensions(profileGroupConfig, formats);

        Map<String, String> expectedExtensions = new HashMap<String, String>() {{
            put(StatsProfileGroupAcc.START_PERIOD_EXTENSION, Long.toString(startPeriod));
            put(StatsProfileGroupAcc.END_PERIOD_EXTENSION, Long.toString(endPeriod));
            put(STATS_RESULT_NAME.concat(StatsProfileGroupAcc.MIN_RESULT_SUFFIX), formats.get(STATS_RESULT_NAME).format(expectedMin));
            put(STATS_RESULT_NAME.concat(StatsProfileGroupAcc.MAX_RESULT_SUFFIX),  formats.get(STATS_RESULT_NAME).format(expectedMax));
            put(STATS_RESULT_NAME.concat(StatsProfileGroupAcc.MEAN_RESULT_SUFFIX),  formats.get(STATS_RESULT_NAME).format(expectedMean));
            put(STATS_RESULT_NAME.concat(StatsProfileGroupAcc.STDDEV_RESULT_SUFFIX),  formats.get(STATS_RESULT_NAME).format(expectedStdDev));
        }};
        Assert.assertEquals(expectedExtensions, actualExtensions);
        Assert.assertEquals(endPeriod, acc.getEndTimestamp());
    }
}