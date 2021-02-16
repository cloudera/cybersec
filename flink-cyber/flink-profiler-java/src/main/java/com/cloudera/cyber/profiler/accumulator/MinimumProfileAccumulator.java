package com.cloudera.cyber.profiler.accumulator;

import java.text.DecimalFormat;

public class MinimumProfileAccumulator extends FieldProfileAccumulator {
    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("#.##");

    public MinimumProfileAccumulator(String resultExtensionName, String fieldName, DecimalFormat format) {
        super(resultExtensionName, fieldName, Double.MAX_VALUE, format != null ? format : DEFAULT_FORMAT);
    }

    @Override
    protected void updateProfileValue(double fieldValue) {
        profileValue = Math.min(profileValue, fieldValue);
    }


    @Override
    public void merge(ProfileAccumulator other) {
        if (other instanceof MinimumProfileAccumulator) {
            profileValue = Math.min(profileValue, ((MinimumProfileAccumulator) other).profileValue);
        }
    }
}
