package com.cloudera.cyber.profiler;

import lombok.*;
import org.apache.flink.util.Preconditions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Builder
public class ProfileGroupConfig implements Serializable {
    public static final String ANY_SOURCE = "ANY";
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
        Preconditions.checkNotNull(profileGroupName, "profileGroupName is null");
        Preconditions.checkState(!profileGroupName.isEmpty());
        verifyList(sources, "%s sources list -  specify a list of source names or ANY matching any source");
        verifyList(keyFieldNames, "%S key field list - specify the name of one or more key fields for the profile");
        Preconditions.checkNotNull(periodDuration, "period duration is not specified");
        Preconditions.checkNotNull(periodDurationUnit, "period duration unit is  not specified");
        verifyList(measurements, "%s measurements list - specify at least on measurement for the profile group");
    }

    private void verifyList(List<?> listToVerify, String messageFormat) {
        Preconditions.checkNotNull(listToVerify, String.format(messageFormat, "null"));
        Preconditions.checkArgument(!listToVerify.isEmpty(), String.format(messageFormat, "empty"));
    }
}
