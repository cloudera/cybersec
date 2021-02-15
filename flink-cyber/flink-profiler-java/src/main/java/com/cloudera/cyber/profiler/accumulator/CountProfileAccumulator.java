package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.Message;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.text.DecimalFormat;

@ToString(callSuper=true)
@EqualsAndHashCode(callSuper = true)
public class CountProfileAccumulator extends DoubleProfileAccumulator {
    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("#");
    double count = 0;

    public CountProfileAccumulator(String resultExtensionName, DecimalFormat format) {
        super(resultExtensionName, 0, format != null ? format : DEFAULT_FORMAT);
    }

    @Override
    public void add(Message message) {
        profileValue += 1;
    }

    @Override
    public void merge(ProfileAccumulator other) {
        if (this != other && other instanceof CountProfileAccumulator) {
            profileValue += ((CountProfileAccumulator) other).profileValue;
        }
    }

}
