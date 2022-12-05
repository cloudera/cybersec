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

import com.cloudera.cyber.profiler.ProfileMeasurementConfig;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Builder
public class MeasurementDto implements Serializable {
    private Integer id;
    private Integer profileId;
    private String fieldName;
    private String resultExtensionName;
    private String aggregationMethod;
    private Boolean calculateStats;
    private String format;
    private Long firstSeenExpirationDuration;
    private String firstSeenExpirationDurationUnit;

    public static MeasurementDto of(ProfileMeasurementConfig config) {
        return MeasurementDto.builder()
                .fieldName(config.getFieldName())
                .resultExtensionName(config.getResultExtensionName())
                .aggregationMethod(config.getAggregationMethod().toString())
                .calculateStats(config.getCalculateStats())
                .format(config.getFormat())
                .firstSeenExpirationDuration(config.getFirstSeenExpirationDuration())
                .firstSeenExpirationDurationUnit(config.getFirstSeenExpirationDurationUnit())
                .build();
    }
}
