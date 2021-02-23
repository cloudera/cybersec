package com.cloudera.cyber.profiler;

import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import com.cloudera.cyber.profiler.accumulator.FieldValueProfileGroupAcc;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldValueProfileAggregateFunction extends ProfileAggregateFunction {

    public FieldValueProfileAggregateFunction(ProfileGroupConfig profileGroupConfig) {
        super(profileGroupConfig, profileGroupConfig.getProfileGroupName());
    }

    @Override
    public ProfileGroupAcc createAccumulator() {
        return new FieldValueProfileGroupAcc(profileGroupConfig);
    }

    @Override
    protected Map<String, DecimalFormat> getMeasurementFormats() {
        return profileGroupConfig.getMeasurements().stream().
                collect(Collectors.toMap(ProfileMeasurementConfig::getResultExtensionName,
                        ProfileMeasurementConfig::getDecimalFormat));
    }
}
