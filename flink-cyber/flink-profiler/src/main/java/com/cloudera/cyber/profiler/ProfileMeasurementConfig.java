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

import lombok.*;
import org.apache.flink.util.Preconditions;

import java.io.Serializable;
import java.text.DecimalFormat;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
public class ProfileMeasurementConfig implements Serializable {
    public static final String NULL_FIELD_VALUE_ERROR = "Profile group %s: measurement %d has a null %s";
    public static final String EMPTY_FIELD_VALUE_ERROR = "Profile group %s: measurement %d has an empty %s";
    public static final String FIRST_SEEN_ON_NUMERIC = "Profile group %s: measurement offset %d has firstSeenExpiration but the aggregationMethod is not FIRST_SEEN.";
    private Integer id;
    private String fieldName;
    private String resultExtensionName;
    private ProfileAggregationMethod aggregationMethod;
    private Boolean calculateStats;
    private String format;
    private Long firstSeenExpirationDuration;
    private String firstSeenExpirationDurationUnit;

    public boolean hasStats() {
        return (calculateStats != null && calculateStats);
    }

    public DecimalFormat getDecimalFormat() {
        if (format != null) {
            return new DecimalFormat(format);
        } else {
            return ProfileAggregationMethod.defaultFormat.get(aggregationMethod);
        }
    }

    public void verify(ProfileGroupConfig profileGroupConfig, int offset) {
        String profileGroupName = profileGroupConfig.getProfileGroupName();
        Preconditions.checkNotNull(aggregationMethod, String.format(NULL_FIELD_VALUE_ERROR, profileGroupName, offset, "aggregationMethod"));
        if (!aggregationMethod.equals(ProfileAggregationMethod.COUNT) && !aggregationMethod.equals(ProfileAggregationMethod.FIRST_SEEN)) {
            checkString(profileGroupName, offset, "fieldName", fieldName);
        }
        checkString(profileGroupName, offset, "resultExtensionName", resultExtensionName);
        if (ProfileAggregationMethod.FIRST_SEEN.equals(aggregationMethod)) {
            if (firstSeenExpirationDuration != null || firstSeenExpirationDurationUnit != null ) {
                profileGroupConfig.verifyTime("firstSeenExpirationDuration", firstSeenExpirationDuration, firstSeenExpirationDurationUnit);
            }
         } else {
            Preconditions.checkState(firstSeenExpirationDuration == null && firstSeenExpirationDurationUnit == null, String.format(FIRST_SEEN_ON_NUMERIC, profileGroupName, offset));
        }

    }

    private void checkString(String profileGroupName, int offset, String name, String value) {
        Preconditions.checkNotNull(value, String.format(NULL_FIELD_VALUE_ERROR, profileGroupName, offset, name));
        Preconditions.checkState(!value.isEmpty(), String.format(EMPTY_FIELD_VALUE_ERROR, profileGroupName, offset, name));
    }
}
