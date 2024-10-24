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

import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ProfileGroupTest {
    protected static final String KEY_FIELD_NAME = "key_field";
    protected static final String KEY_FIELD_VALUE = "key_field_value";
    protected static final String PROFILE_GROUP_NAME = "test_profile";
    protected static final String RESULT_EXTENSION_NAME = "result";
    protected static final String SUM_FIELD_NAME = "field_to_sum";


    protected ProfileGroupConfig getProfileGroupConfig(List<ProfileMeasurementConfig> measurements) {
        return ProfileGroupConfig.builder().profileGroupName(ProfileGroupTest.PROFILE_GROUP_NAME).keyFieldNames(Lists.newArrayList(KEY_FIELD_NAME))
                .sources(Lists.newArrayList("ANY"))
                        .periodDuration(1L).periodDurationUnit(TimeUnit.MINUTES.name())
                        .measurements(Lists.newArrayList(measurements))
                        .build();
    }

    protected void addMessage(ProfileGroupAcc profileGroupAccumulator, long timestamp, long aggregationFieldValue, ProfileGroupConfig profileGroupConfig) {
        profileGroupAccumulator.addMessage(createMessage(timestamp, aggregationFieldValue), profileGroupConfig);
    }

    protected ProfileMessage createMessage(long timestamp, long aggregationFieldValue) {
        Map<String, String> extensions = ImmutableMap.of(
                KEY_FIELD_NAME, KEY_FIELD_VALUE,
                SUM_FIELD_NAME, Long.toString(aggregationFieldValue)
        );
        return new ProfileMessage(timestamp, extensions);
    }
}
