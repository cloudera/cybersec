package com.cloudera.cyber.profiler.accumulator;

import java.text.DecimalFormat;

public class MaximumProfileAccumulator extends FieldProfileAccumulator {

    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("#.##");

    public MaximumProfileAccumulator(String resultExtensionName, String fieldName, DecimalFormat format) {
        super(resultExtensionName, fieldName, Double.MIN_VALUE, format != null ? format : DEFAULT_FORMAT);
    }

    @Override
    protected void updateProfileValue(double fieldValue) {
        profileValue = Math.max(fieldValue, profileValue);
    }

    @Override
    public void merge(ProfileAccumulator other) {
        if (other instanceof MaximumProfileAccumulator) {
            profileValue = Math.max(profileValue, ((MaximumProfileAccumulator) other).profileValue);
        }
    }
}
