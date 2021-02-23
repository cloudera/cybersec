package com.cloudera.cyber.profiler;

import lombok.*;

import java.io.Serializable;
import java.text.DecimalFormat;

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

    public DecimalFormat getDecimalFormat() {
        if (format != null) {
            return new DecimalFormat(format);
        } else {
            return ProfileAggregationMethod.defaultFormat.get(aggregationMethod);
        }
    }
}
