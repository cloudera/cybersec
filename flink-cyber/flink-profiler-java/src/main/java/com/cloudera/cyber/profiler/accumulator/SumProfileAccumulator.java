package com.cloudera.cyber.profiler.accumulator;


import java.text.DecimalFormat;

public class SumProfileAccumulator extends FieldProfileAccumulator {
    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("#.##");

    public SumProfileAccumulator(String resultExtensionName, String extensionName, DecimalFormat format) {
        super(resultExtensionName, extensionName, 0, format != null ? format : DEFAULT_FORMAT);
    }

    @Override
    protected void updateProfileValue(double fieldValue) {
        profileValue += fieldValue;
    }

    @Override
    public void merge(ProfileAccumulator other) {
        if (this != other && other instanceof SumProfileAccumulator) {
            profileValue += ((SumProfileAccumulator)other).profileValue;
        }
    }
}
