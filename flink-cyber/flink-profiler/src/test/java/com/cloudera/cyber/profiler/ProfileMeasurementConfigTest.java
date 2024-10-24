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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Test;

public class ProfileMeasurementConfigTest {
    private static final int TEST_OFFSET = 1;
    private static final String TEST_PROFILE_GROUP = "test_profile_group";

    @Test
    public void testValidConfigs() {
        ProfileAggregationMethod aggregationMethod = ProfileAggregationMethod.COUNT_DISTINCT;
        ProfileMeasurementConfig measurementConfig = ProfileMeasurementConfig.builder()
                .fieldName("field")
                .resultExtensionName("result")
                .aggregationMethod(aggregationMethod)
                .build();
        verifyGoodConfig(measurementConfig);
        Assert.assertFalse(measurementConfig.hasStats());
        Assert.assertEquals(ProfileAggregationMethod.defaultFormat.get(ProfileAggregationMethod.COUNT_DISTINCT), measurementConfig.getDecimalFormat());

        measurementConfig = ProfileMeasurementConfig.builder()
                .fieldName("field")
                .resultExtensionName("result")
                .aggregationMethod(aggregationMethod)
                .calculateStats(true)
                .build();
        verifyGoodConfig(measurementConfig);
        Assert.assertTrue(measurementConfig.hasStats());
        Assert.assertEquals(ProfileAggregationMethod.defaultFormat.get(ProfileAggregationMethod.COUNT_DISTINCT), measurementConfig.getDecimalFormat());

        String formatString = "##";
        measurementConfig = ProfileMeasurementConfig.builder()
                .fieldName("field")
                .resultExtensionName("result")
                .aggregationMethod(aggregationMethod)
                .format(formatString)
                .build();
        verifyGoodConfig(measurementConfig);
        Assert.assertFalse(measurementConfig.hasStats());
        Assert.assertEquals(new DecimalFormat(formatString), measurementConfig.getDecimalFormat());

        measurementConfig = ProfileMeasurementConfig.builder()
                .fieldName("field")
                .resultExtensionName("result")
                .aggregationMethod(ProfileAggregationMethod.FIRST_SEEN)
                .build();
        verifyGoodConfig(measurementConfig);
        Assert.assertFalse(measurementConfig.hasStats());
        Assert.assertEquals(new DecimalFormat(formatString), measurementConfig.getDecimalFormat());

        measurementConfig = ProfileMeasurementConfig.builder()
                .fieldName("field")
                .resultExtensionName("result")
                .aggregationMethod(ProfileAggregationMethod.FIRST_SEEN)
                .firstSeenExpirationDuration(60L).firstSeenExpirationDurationUnit(TimeUnit.MINUTES.name())
                .build();
        verifyGoodConfig(measurementConfig);
        Assert.assertFalse(measurementConfig.hasStats());
        Assert.assertEquals(new DecimalFormat(formatString), measurementConfig.getDecimalFormat());
    }

    private void verifyGoodConfig(ProfileMeasurementConfig measurementConfig) {
        ArrayList<ProfileMeasurementConfig> measurements = new ArrayList<>(Collections.singleton(measurementConfig));
        ProfileGroupConfig config = ProfileGroupConfig.builder().profileGroupName(TEST_PROFILE_GROUP)
                .measurements(measurements).build();
        measurementConfig.verify(config, TEST_OFFSET);
    }

    @Test
    public void testIllegalFieldName() {
        String fieldName = "fieldName";
        ProfileMeasurementConfig nullFieldName = ProfileMeasurementConfig.builder().aggregationMethod(ProfileAggregationMethod.COUNT_DISTINCT).build();
        testNullField(nullFieldName, String.format(ProfileMeasurementConfig.NULL_FIELD_VALUE_ERROR, TEST_PROFILE_GROUP, TEST_OFFSET, fieldName));

        ProfileMeasurementConfig emptyFieldName = ProfileMeasurementConfig.builder().aggregationMethod(ProfileAggregationMethod.COUNT_DISTINCT).fieldName("").build();
        testIllegalField(emptyFieldName, String.format(ProfileMeasurementConfig.EMPTY_FIELD_VALUE_ERROR, TEST_PROFILE_GROUP, TEST_OFFSET, fieldName));
    }

    @Test
    public void testIllegalResultExtension() {
        String resultExtensionFieldName =  "resultExtensionName";
        ProfileMeasurementConfig nullResultExtension = ProfileMeasurementConfig.builder().fieldName("valid_field").aggregationMethod(ProfileAggregationMethod.MAX).build();
        testNullField(nullResultExtension, String.format(ProfileMeasurementConfig.NULL_FIELD_VALUE_ERROR, TEST_PROFILE_GROUP, TEST_OFFSET, resultExtensionFieldName));

        ProfileMeasurementConfig emptyResultExtension = ProfileMeasurementConfig.builder().fieldName("valid_field").aggregationMethod(ProfileAggregationMethod.MAX).resultExtensionName("").build();
        testIllegalField(emptyResultExtension, String.format(ProfileMeasurementConfig.EMPTY_FIELD_VALUE_ERROR, TEST_PROFILE_GROUP, TEST_OFFSET, resultExtensionFieldName));
    }

    @Test
    public void testNullAggregationMethod() {
        ProfileMeasurementConfig nullAggregation = ProfileMeasurementConfig.builder().fieldName("valid_field")
                .resultExtensionName("valid_extension_name").build();
        testNullField(nullAggregation, String.format(ProfileMeasurementConfig.NULL_FIELD_VALUE_ERROR, TEST_PROFILE_GROUP, TEST_OFFSET, "aggregationMethod"));
    }

    @Test
    public void testFirstSeenDurationOnNumeric() {
        ProfileMeasurementConfig numericWithFirstSeenDuration = ProfileMeasurementConfig.builder()
                .fieldName("valid_field").resultExtensionName("valid_result_ext")
                .aggregationMethod(ProfileAggregationMethod.COUNT)
                .firstSeenExpirationDuration(10L)
                .build();
        testIllegalField(numericWithFirstSeenDuration, String.format(ProfileMeasurementConfig.FIRST_SEEN_ON_NUMERIC, TEST_PROFILE_GROUP, TEST_OFFSET));

        ProfileMeasurementConfig numericWithFirstSeenDurationUnit = ProfileMeasurementConfig.builder()
                .fieldName("valid_field").resultExtensionName("valid_result_ext")
                .aggregationMethod(ProfileAggregationMethod.COUNT)
                .firstSeenExpirationDurationUnit(TimeUnit.HOURS.name())
                .build();
        testIllegalField(numericWithFirstSeenDurationUnit, String.format(ProfileMeasurementConfig.FIRST_SEEN_ON_NUMERIC, TEST_PROFILE_GROUP, TEST_OFFSET));

        ProfileMeasurementConfig numericWithFirstSeenDurationAndUnit = ProfileMeasurementConfig.builder()
                .fieldName("valid_field").resultExtensionName("valid_result_ext")
                .aggregationMethod(ProfileAggregationMethod.COUNT)
                .firstSeenExpirationDuration(10L).firstSeenExpirationDurationUnit(TimeUnit.HOURS.name())
                .build();
        testIllegalField(numericWithFirstSeenDurationAndUnit, String.format(ProfileMeasurementConfig.FIRST_SEEN_ON_NUMERIC, TEST_PROFILE_GROUP, TEST_OFFSET));
    }

    @Test
    public void testIllegalFirstSeenDurationAndUnit() {
        ProfileMeasurementConfig missingUnit = ProfileMeasurementConfig.builder()
                .fieldName("valid_field").resultExtensionName("valid_result_ext")
                .aggregationMethod(ProfileAggregationMethod.FIRST_SEEN)
                .firstSeenExpirationDuration(10L)
                .build();
        testNullField(missingUnit, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, TEST_PROFILE_GROUP, "firstSeenExpirationDurationUnit", "null"));

        ProfileMeasurementConfig missingDuration = ProfileMeasurementConfig.builder()
                .fieldName("valid_field").resultExtensionName("valid_result_ext")
                .aggregationMethod(ProfileAggregationMethod.FIRST_SEEN)
                .firstSeenExpirationDurationUnit(TimeUnit.HOURS.name())
                .build();
        testNullField(missingDuration, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, TEST_PROFILE_GROUP, "firstSeenExpirationDuration", "null"));

        ProfileMeasurementConfig illegalUnits = ProfileMeasurementConfig.builder()
                .fieldName("valid_field").resultExtensionName("valid_result_ext")
                .aggregationMethod(ProfileAggregationMethod.FIRST_SEEN)
                .firstSeenExpirationDuration(10L).firstSeenExpirationDurationUnit("illegal unit")
                .build();
        testIllegalField(illegalUnits, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, TEST_PROFILE_GROUP, "firstSeenExpirationDurationUnit", "not a legal time unit"));

        ProfileMeasurementConfig negativeDuration = ProfileMeasurementConfig.builder()
                .fieldName("valid_field").resultExtensionName("valid_result_ext")
                .aggregationMethod(ProfileAggregationMethod.FIRST_SEEN)
                .firstSeenExpirationDuration(-8L).firstSeenExpirationDurationUnit(TimeUnit.HOURS.name())
                .build();
        testIllegalField(negativeDuration, String.format(ProfileGroupConfig.PROFILE_TIME_ERROR, TEST_PROFILE_GROUP, "firstSeenExpirationDuration", "0 or negative"));

    }

    private void testNullField(ProfileMeasurementConfig badMeasurement, String expectedMessage) {
        testFieldError(badMeasurement, expectedMessage, NullPointerException.class);
    }

    private void testIllegalField(ProfileMeasurementConfig badMeasurement, String expectedMessage) {
        testFieldError(badMeasurement, expectedMessage, IllegalStateException.class);
    }

    private void testFieldError(ProfileMeasurementConfig badMeasurement, String expectedMessage, Class<?> expectedException) {
        ArrayList<ProfileMeasurementConfig> measurements = new ArrayList<>(Collections.singletonList(badMeasurement));
        ProfileGroupConfig config = ProfileGroupConfig.builder().profileGroupName(TEST_PROFILE_GROUP).measurements(measurements).build();
        assertThatThrownBy(() -> badMeasurement.verify(config, TEST_OFFSET))
                .isInstanceOf(expectedException)
                .hasMessage(expectedMessage);
    }

}
