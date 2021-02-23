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
