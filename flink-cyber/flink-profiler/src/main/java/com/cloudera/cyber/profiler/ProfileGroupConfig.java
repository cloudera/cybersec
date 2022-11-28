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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Builder
public class ProfileGroupConfig implements Serializable {
    public static final String ANY_SOURCE = "ANY";

    static final String NULL_PROFILE_GROUP_NAME_ERROR = "profileGroupName is null";
    static final String EMPTY_PROFILE_GROUP_NAME_ERROR = "profileGroupName cannot be empty";
    public static final String EMPTY_SOURCES_ERROR = "Profile group '%s': %s sources list -  specify a list of source names or ANY matching any source";
    public static final String EMPTY_KEY_FIELDS_ERROR = "Profile group '%s': %s key field list - specify the name of one or more key fields for the profile";
    public static final String PROFILE_TIME_ERROR = "Profile group '%s': %s is %s";
    public static final String NULL_EMPTY_MEASUREMENTS_ERROR = "Profile group '%s': %s measurements list - specify at least on measurement for the profile group";
    public static final String MISSING_STATS_SLIDE = "Profile group '%s' has calculateStats enabled but does not specify statsSlide";
    public static final String UNNECESSARY_STATS_SLIDE_ERROR = "Profile group '%s' does not have calculateStats enabled but specifies statsSlide";
    public static final String MISSING_STATS_SLIDE_UNIT = "Profile group '%s' has calculateStates enabled but does not specify statsSlideUnit";
    public static final String ILLEGAL_TIME_UNIT_ERROR = "Profile group '%s' %s has an undefined time unit.";
    private String profileGroupName;
    private ArrayList<String> sources;
    private ArrayList<String> keyFieldNames;
    private Long periodDuration;
    private String periodDurationUnit;
    private Long statsSlide;
    private String statsSlideUnit;
    private ArrayList<ProfileMeasurementConfig> measurements;
    private Long latenessTolerance;

    public boolean hasStats() {
        return measurements.stream().anyMatch(ProfileMeasurementConfig::hasStats);
    }

    public boolean needsSourceFilter() {
         return !sources.contains(ANY_SOURCE);
    }

    public List<String> getMeasurementFieldNames() {
        return measurements.stream().map(ProfileMeasurementConfig::getFieldName).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public boolean hasFirstSeen() {
        return measurements.stream().anyMatch(m -> m.getAggregationMethod().equals(ProfileAggregationMethod.FIRST_SEEN));
    }

    public void verify() {
        Preconditions.checkNotNull(profileGroupName, NULL_PROFILE_GROUP_NAME_ERROR);
        Preconditions.checkState(!profileGroupName.isEmpty(), EMPTY_PROFILE_GROUP_NAME_ERROR);
        verifyList(sources, EMPTY_SOURCES_ERROR);
        verifyList(keyFieldNames, EMPTY_KEY_FIELDS_ERROR);
        verifyTime("periodDuration", periodDuration, periodDurationUnit);
        verifyList(measurements, NULL_EMPTY_MEASUREMENTS_ERROR);
        if (hasStats()) {
            verifyTime("statsSlide", statsSlide, statsSlideUnit);
        } else {
            Preconditions.checkState(statsSlide == null && statsSlideUnit == null, String.format(UNNECESSARY_STATS_SLIDE_ERROR, getProfileGroupName()));
        }

        int offset = 1;
        for(ProfileMeasurementConfig measurement : measurements) {
            measurement.verify(this, offset++);
        }
    }

    private void verifyList(List<?> listToVerify, String messageFormat) {
        Preconditions.checkNotNull(listToVerify, String.format(messageFormat, getProfileGroupName(), "null"));
        Preconditions.checkArgument(!listToVerify.isEmpty(), String.format(messageFormat, getProfileGroupName(), "empty"));
    }

    public void verifyTime(String baseTimeFieldName, Long profileTimeDuration, String profileTimeUnit) {
        final String timeUnitFieldName = baseTimeFieldName.concat("Unit");
        // check missing duration and unit
        Preconditions.checkNotNull(profileTimeDuration, String.format(PROFILE_TIME_ERROR, getProfileGroupName(), baseTimeFieldName, "null"));
        Preconditions.checkNotNull(profileTimeUnit, String.format(PROFILE_TIME_ERROR, getProfileGroupName(), timeUnitFieldName, "null"));
        // time unit must match one of the define java time units
        Preconditions.checkState(Stream.of(TimeUnit.values()).anyMatch(unit -> unit.name().equals(profileTimeUnit)),
                String.format(PROFILE_TIME_ERROR, getProfileGroupName(), timeUnitFieldName, "not a legal time unit"));
        Preconditions.checkState(profileTimeDuration > 0, String.format(PROFILE_TIME_ERROR, getProfileGroupName(), baseTimeFieldName, "0 or negative"));
    }
}
