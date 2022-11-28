/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.profiler.accumulator;

import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.junit.Assert;
import org.junit.Test;

public class StatsAccTest {

    @Test
    public void testStatsAcc() {
        StatsAcc acc = new StatsAcc();
        verifyResults(acc, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
        acc.add(5.00);
        acc.add(10.00);
        acc.add(20.00);

        verifyResults(acc, 5D, 20D, 11.67, 7.64);

        StatsAcc copy = (StatsAcc)acc.clone();
        verifyResults(copy, 5D, 20D, 11.67, 7.64);

        acc.resetLocal();
        verifyResults(acc, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    @Test
    public void testMergeAcc() {
        StatsAcc acc1 = new StatsAcc();
        acc1.add(50.00);

        StatsAcc acc2 = new StatsAcc();
        acc2.add(1000.00);

        acc1.merge(acc2);
        verifyResults(acc1, 50, 1000, 525, 671.75);
        verifyResults(acc2, 1000, 1000, 1000, 0);
    }

    private static void verifyResults(StatsAcc acc, double min, double max, double mean, double stddev) {
        StatisticalSummaryValues summary  = AggregateSummaryStatistics.aggregate(acc.getLocalValue());
        Assert.assertEquals(min, summary.getMin(), 0.1);
        Assert.assertEquals(max, summary.getMax(), 0.1);
        Assert.assertEquals(mean, summary.getMean(), 0.1);
        Assert.assertEquals(stddev, summary.getStandardDeviation(), 0.1);
    }
}
