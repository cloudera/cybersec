package com.cloudera.cyber.profiler;

import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAcc;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsProfileAggregateFunction extends ProfileAggregateFunction {

    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("0.00");
    public static final String STATS_PROFILE_GROUP_SUFFIX = ".stats";

    public StatsProfileAggregateFunction(ProfileGroupConfig profileGroupConfig) {
        super(profileGroupConfig, profileGroupConfig.getProfileGroupName().concat(STATS_PROFILE_GROUP_SUFFIX));
    }

    @Override
    public ProfileGroupAcc createAccumulator() {
        return new StatsProfileGroupAcc(profileGroupConfig);
    }

    protected Map<String, DecimalFormat> getMeasurementFormats() {
        return profileGroupConfig.getMeasurements().stream().filter(ProfileMeasurementConfig::hasStats).
                collect(Collectors.toMap(ProfileMeasurementConfig::getResultExtensionName, v -> DEFAULT_FORMAT));
    }

}
