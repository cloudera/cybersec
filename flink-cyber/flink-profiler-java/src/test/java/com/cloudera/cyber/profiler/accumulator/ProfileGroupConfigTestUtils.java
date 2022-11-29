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

import com.cloudera.cyber.profiler.ProfileAggregationMethod;
import com.cloudera.cyber.profiler.ProfileMeasurementConfig;

public class ProfileGroupConfigTestUtils {

    public static final String TEST_PROFILE_GROUP = "test_profile";
    public static final String KEY_1 = "key1";
    public static final String KEY_1_VALUE = "key1_value";
    public static final String KEY_2_VALUE = "key2_value";
    public static final String KEY_2 = "key2";

    public static ProfileMeasurementConfig createMeasurement(ProfileAggregationMethod aggMethod, String resultName, String fieldName) {
        return createMeasurement(aggMethod, resultName, fieldName, null);
    }

    public static ProfileMeasurementConfig createMeasurement(ProfileAggregationMethod aggMethod, String resultName, String fieldName, String format) {
        return ProfileMeasurementConfig.builder().aggregationMethod(aggMethod).
                resultExtensionName(resultName).fieldName(fieldName).format(format).build();
    }

    public static ProfileMeasurementConfig createMeasurement(ProfileAggregationMethod aggMethod, String resultName, String fieldName, String format, boolean calculateStats) {
        return ProfileMeasurementConfig.builder().aggregationMethod(aggMethod).
                resultExtensionName(resultName).fieldName(fieldName).format(format).
                calculateStats(calculateStats).build();
    }

}
