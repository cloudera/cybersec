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
