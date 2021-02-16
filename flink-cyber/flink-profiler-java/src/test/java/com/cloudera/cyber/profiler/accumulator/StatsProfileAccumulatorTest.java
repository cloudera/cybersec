package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.TestUtils;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import static com.cloudera.cyber.profiler.accumulator.StatsProfileAccumulator.*;

public class StatsProfileAccumulatorTest {
    private static final String STATS_FIELD_NAME = "stats";
    private static final String STATS_RESULT_NAME = "result";
    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("0.00");

    @Test
    public void testStatsAggregatorWithDefaultFormat() {
        StatsProfileAccumulator acc = new StatsProfileAccumulator(STATS_RESULT_NAME, STATS_FIELD_NAME, null);
        testAddMessage(acc, 50, DEFAULT_FORMAT, 50, 50, 50, 0);
        testAddMessage(acc, 25, DEFAULT_FORMAT, 25, 50, 37.5, 17.68);
        testAddMessage(acc, 100, DEFAULT_FORMAT, 25, 100, 58.33, 38.19);
        testAddMessage(acc, "not a string", DEFAULT_FORMAT, 25, 100, 58.33, 38.19);
    }

    @Test
    public void testStatsAggregatorWithCustomFormat() {
        DecimalFormat customFormat = new DecimalFormat("#");
        StatsProfileAccumulator acc = new StatsProfileAccumulator(STATS_RESULT_NAME, STATS_FIELD_NAME, customFormat);
        testAddMessage(acc, 50, customFormat, 50, 50, 50, 0);
        testAddMessage(acc, 25, customFormat, 25, 50, 38, 18);
        testAddMessage(acc, 100, customFormat, 25, 100, 58, 38);
    }

    @Test
    public void testStatsAggegatorMerge() {
        StatsProfileAccumulator acc1 = new StatsProfileAccumulator(STATS_RESULT_NAME, STATS_FIELD_NAME, null);
        testAddMessage(acc1, 50, DEFAULT_FORMAT, 50, 50, 50, 0);
        testAddMessage(acc1, 25, DEFAULT_FORMAT, 25, 50, 37.5, 17.68);

        StatsProfileAccumulator acc2 = new StatsProfileAccumulator(STATS_RESULT_NAME, STATS_FIELD_NAME, null);
        testAddMessage(acc2, 1000, DEFAULT_FORMAT, 1000, 1000, 1000, 0);
        testAddMessage(acc2, 5, DEFAULT_FORMAT, 5, 1000, 502.5, 703.57);

        acc1.merge(acc2);
        verifyStatsResult(acc1, DEFAULT_FORMAT, 5, 1000, 270, 487.01);
    }

    @Test
    public void testStatsAggregatorErrors() {
        StatsProfileAccumulator acc1 = new StatsProfileAccumulator(STATS_RESULT_NAME, STATS_FIELD_NAME, null);
        testAddMessage(acc1, 50, DEFAULT_FORMAT, 50, 50, 50, 0);

        acc1.merge(null);
        verifyStatsResult(acc1, DEFAULT_FORMAT, 50, 50, 50, 0);

        acc1.merge(acc1);
        verifyStatsResult(acc1, DEFAULT_FORMAT, 50, 50, 50, 0);

        acc1.merge(new CountProfileAccumulator("invalid", null));
        verifyStatsResult(acc1, DEFAULT_FORMAT, 50, 50, 50, 0);

    }

    private void testAddMessage(StatsProfileAccumulator acc, double statsFieldValue, DecimalFormat format,
                                double expectedMin, double expectedMax, double expectedMean, double expectedStdDev) {
        testAddMessage(acc, format.format(statsFieldValue), format, expectedMin, expectedMax, expectedMean, expectedStdDev);
    }

    private void testAddMessage(StatsProfileAccumulator acc, String statsFieldValue, DecimalFormat format,
                                double expectedMin, double expectedMax, double expectedMean, double expectedStdDev) {
        acc.add(TestUtils.createMessage(ImmutableMap.of(STATS_FIELD_NAME, statsFieldValue)));
        verifyStatsResult(acc, format, expectedMin, expectedMax, expectedMean, expectedStdDev);
    }

    private static void verifyStatsResult(StatsProfileAccumulator acc, DecimalFormat format, double expectedMin, double expectedMax, double expectedMean, double expectedStdDev) {
        Map<String, String> actualExtensions = new HashMap<>();
        acc.addResults(actualExtensions);

        Map<String, String> expectedExtensions = new HashMap<String, String>() {{
            put(STATS_RESULT_NAME.concat(MIN_RESULT_SUFFIX), format.format(expectedMin));
            put(STATS_RESULT_NAME.concat(MAX_RESULT_SUFFIX), format.format(expectedMax));
            put(STATS_RESULT_NAME.concat(MEAN_RESULT_SUFFIX), format.format(expectedMean));
            put(STATS_RESULT_NAME.concat(STDDEV_RESULT_SUFFIX), format.format(expectedStdDev));
        }};
        Assert.assertEquals(expectedExtensions, actualExtensions);
    }
}