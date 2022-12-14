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

package com.cloudera.cyber.profiler;

import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAccTest;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.Map;

import static com.cloudera.cyber.profiler.ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION;
import static com.cloudera.cyber.profiler.StatsProfileAggregateFunction.STATS_PROFILE_GROUP_SUFFIX;
import static com.cloudera.cyber.profiler.accumulator.FieldValueProfileGroupAccTest.getFormats;
import static com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAcc.*;
import static com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAccTest.NO_STATS_RESULT_NAME;
import static com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAccTest.STATS_RESULT_NAME;

public class StatsProfileAggregateFunctionTest {

    private static final String MIN_STATS_RESULT = STATS_RESULT_NAME.concat(MIN_RESULT_SUFFIX);
    private static final String MAX_STATS_RESULT = STATS_RESULT_NAME.concat(MAX_RESULT_SUFFIX);
    private static final String MEAN_STATUS_RESULT = STATS_RESULT_NAME.concat(MEAN_RESULT_SUFFIX);
    private static final String STDDEV_STATUS_RESULT = STATS_RESULT_NAME.concat(STDDEV_RESULT_SUFFIX);

    @Test
    public void testAggregateFunction() {
        ProfileGroupConfig profileGroupConfig = StatsProfileGroupAccTest.createProfileGroup();
        StatsProfileAggregateFunction agg = new StatsProfileAggregateFunction(profileGroupConfig);
        ProfileGroupAcc acc = agg.createAccumulator();

        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        ProfileMessage profileMessage = getProfileMessage(agg, acc, currentTimestamp, "100", "3");
        verifyProfileMessage(profileGroupConfig, profileMessage, currentTimestamp, currentTimestamp,
                100, 100, 100, 0);

        profileMessage = getProfileMessage(agg, acc, currentTimestamp + 1, "1000", "20000");
        verifyProfileMessage(profileGroupConfig, profileMessage, currentTimestamp, currentTimestamp+ 1,
                100, 1000, 550, 636.4);

        ProfileGroupAcc acc1 = agg.createAccumulator();
        profileMessage = getProfileMessage(agg, acc1, currentTimestamp - 1, "100000", "30");
        verifyProfileMessage(profileGroupConfig, profileMessage, currentTimestamp - 1, currentTimestamp - 1,
               100000, 100000, 100000, 0 );

        agg.merge(acc, acc1);
        verifyProfileMessage(profileGroupConfig, agg.getResult(acc), currentTimestamp - 1, currentTimestamp + 1,
                100, 100000, 33700, 57419.25);
    }

    private ProfileMessage getProfileMessage(ProfileAggregateFunction aggregateFunction, ProfileGroupAcc acc,
                                      long timestamp, String statsFieldValue, String noStatsFieldValue) {

        aggregateFunction.add(new ProfileMessage(timestamp,
                ImmutableMap.of(STATS_RESULT_NAME, statsFieldValue, NO_STATS_RESULT_NAME, noStatsFieldValue)),
                acc);
        return aggregateFunction.getResult(acc);
    }

    private void verifyProfileMessage(ProfileGroupConfig profileGroupConfig, ProfileMessage profileMessage,
                                      long startPeriod, long endPeriod,
                                      double min, double max, double mean, double stddev) {

        Assert.assertEquals(endPeriod, profileMessage.getTs());

        Map<String, DecimalFormat> formats = getFormats(profileGroupConfig);
        DecimalFormat format = formats.get(STATS_RESULT_NAME);
        Map<String, String> actualExtensions = profileMessage.getExtensions();
        Assert.assertEquals(profileGroupConfig.getProfileGroupName().concat(STATS_PROFILE_GROUP_SUFFIX), actualExtensions.get(PROFILE_GROUP_NAME_EXTENSION));
        Assert.assertEquals(Long.toString(startPeriod), actualExtensions.get(START_PERIOD_EXTENSION));
        Assert.assertEquals(Long.toString(endPeriod), actualExtensions.get(END_PERIOD_EXTENSION));
        Assert.assertEquals(format.format(min), actualExtensions.get(MIN_STATS_RESULT));
        Assert.assertEquals(format.format(max), actualExtensions.get(MAX_STATS_RESULT));
        Assert.assertEquals(format.format(mean), actualExtensions.get(MEAN_STATUS_RESULT));
        Assert.assertEquals(format.format(stddev), actualExtensions.get(STDDEV_STATUS_RESULT));

        Assert.assertEquals(7, actualExtensions.size());
    }
}
