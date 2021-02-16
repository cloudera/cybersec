package com.cloudera.cyber.profiler;

import lombok.*;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
public class ProfileMeasurementConfig implements Serializable {
    private String fieldName;
    private String resultExtensionName;
    private ProfileAggregationMethod aggregationMethod;
    private Boolean calculateStats;
    private String format;

    public boolean hasStats() {
        return (calculateStats != null && calculateStats);
    }
}
