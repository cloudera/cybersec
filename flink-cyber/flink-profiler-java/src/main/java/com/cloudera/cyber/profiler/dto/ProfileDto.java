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

package com.cloudera.cyber.profiler.dto;

import com.cloudera.cyber.profiler.ProfileGroupConfig;
import com.google.common.collect.Ordering;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Builder
public class ProfileDto implements Serializable {
    private Integer id;
    private String profileGroupName;
    private String keyFieldNames;
    private Long periodDuration;
    private String periodDurationUnit;
    private Long statsSlide;
    private String statsSlideUnit;
    private ArrayList<MeasurementDto> measurementDtos;

    public static ProfileDto of(ProfileGroupConfig profileGroup) {
        ArrayList<String> keyFieldNames = new ArrayList<>(profileGroup.getKeyFieldNames());
        return ProfileDto.builder()
                .profileGroupName(profileGroup.getProfileGroupName())
                .keyFieldNames(keyFieldNames.stream().sorted().collect(Collectors.joining(",")))
                .measurementDtos(new ArrayList<>(profileGroup.getMeasurements().stream().map(MeasurementDto::of).collect(Collectors.toList())))
                .periodDuration(profileGroup.getPeriodDuration())
                .periodDurationUnit(profileGroup.getPeriodDurationUnit())
                .statsSlide(profileGroup.getStatsSlide())
                .statsSlideUnit(profileGroup.getStatsSlideUnit())
                .build();
    }
}
