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
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.TwoInputStreamOperatorTestHarness;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;


public class ProfileStatsJoinTest extends ProfileGroupTest {

    @Test
    public void testProfileStatsJoin() {
        ProfileStatsJoin join = new ProfileStatsJoin();

        ProfileGroupConfig profileGroupConfig = getProfileGroupConfig();
        ProfileMessage profileMessage = createProfileMessage(profileGroupConfig);
        ProfileMessage statsMessage = createStatsMessage(profileGroupConfig, profileMessage);

        ProfileMessage joinedMessage = join.join(profileMessage, statsMessage);

        Assert.assertEquals(getExpectedJoinedMessage(profileMessage, statsMessage), joinedMessage);
    }

    private ProfileMessage getExpectedJoinedMessage( ProfileMessage message1, ProfileMessage message2) {
        Map<String, String> mergedExtensions = message2.getExtensions().entrySet().stream().
                filter(e -> ProfileStatsJoin.isStatsExtension(e.getKey())).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        message1.getExtensions().forEach(mergedExtensions::putIfAbsent);
        return new ProfileMessage(message1.getTs(), mergedExtensions);
    }

    private ProfileMessage createProfileMessage(ProfileGroupConfig profileGroupConfig) {
        ProfileAggregateFunction aggregateFunction = new FieldValueProfileAggregateFunction(profileGroupConfig);

        ProfileGroupAcc acc = aggregateFunction.createAccumulator();
        long currentTimestamp = MessageUtils.getCurrentTimestamp();
        addMessage(acc, currentTimestamp, 30, profileGroupConfig);
        addMessage(acc, currentTimestamp + 100, 100, profileGroupConfig);

        return aggregateFunction.getResult(acc);
    }

    private ProfileMessage createStatsMessage(ProfileGroupConfig profileGroupConfig, ProfileMessage profileMessage) {
        ProfileAggregateFunction aggregateFunction = new StatsProfileAggregateFunction(profileGroupConfig);
        ProfileGroupAcc acc = aggregateFunction.createAccumulator();
        acc.addMessage(profileMessage, profileGroupConfig);

        return aggregateFunction.getResult(acc);
    }

    private ProfileGroupConfig getProfileGroupConfig() {
        List<ProfileMeasurementConfig> measurements = Collections.singletonList(ProfileMeasurementConfig.builder().resultExtensionName(RESULT_EXTENSION_NAME).
                aggregationMethod(ProfileAggregationMethod.SUM).fieldName(SUM_FIELD_NAME).format("0.000000").calculateStats(true).
                build());

        return getProfileGroupConfig(measurements);
    }
}
