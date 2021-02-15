package com.cloudera.cyber.profiler.accumulator;


import java.text.DecimalFormat;

public abstract class DoubleProfileAccumulator extends SingleValueProfileAccumulator {
    protected double profileValue;
    private DecimalFormat format;

    public DoubleProfileAccumulator(String resultExtensionName, double initialProfileValue, DecimalFormat format) {
        super(resultExtensionName);
        this.profileValue = initialProfileValue;
        this.format = format;
    }

    @Override
    public String getResult() {
        return format.format(profileValue);
    }

}
