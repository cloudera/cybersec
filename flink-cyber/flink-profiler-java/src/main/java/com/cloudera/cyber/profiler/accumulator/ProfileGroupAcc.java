package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.profiler.ProfileGroupConfig;
import org.apache.flink.api.common.accumulators.Accumulator;
import org.apache.flink.api.common.accumulators.LongMaximum;
import org.apache.flink.api.common.accumulators.LongMinimum;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class ProfileGroupAcc {
    public static final String START_PERIOD_EXTENSION = "start_period";
    public static final String END_PERIOD_EXTENSION = "end_period";

    final LongMinimum startPeriodTimestamp = new LongMinimum();
    final LongMaximum endPeriodTimestamp = new LongMaximum();
    final List<Accumulator<?, ? extends Serializable>> accumulators;

    public ProfileGroupAcc(List<Accumulator<?, ? extends Serializable>> accumulators) {
        this.accumulators = accumulators;
    }

    protected abstract void updateAccumulators(Message message, ProfileGroupConfig profileGroupConfig);

    protected abstract void addExtensions(ProfileGroupConfig profileGroupConfig, Map<String, String> extensions, Map<String, DecimalFormat> measurementFormats);

    protected static Double getFieldValueAsDouble(Message message, String fieldName) {
        String extensionValue = message.getExtensions().get(fieldName);
        if (extensionValue != null) {
            try {
                return Double.parseDouble(extensionValue);
            } catch (NumberFormatException ignored) {
                // extension value is not numeric so we cant do stats on it
            }
        }
        return null;
    }

    public void addMessage(Message message, ProfileGroupConfig profileGroupConfig) {
        startPeriodTimestamp.add(message.getTs());
        endPeriodTimestamp.add(message.getTs());
        updateAccumulators(message, profileGroupConfig);
    }

    public Map<String, String> getProfileExtensions(ProfileGroupConfig profileGroupConfig, Map<String, DecimalFormat> measurementFormats) {
        Map<String, String> extensions = new HashMap<>();
        extensions.put(START_PERIOD_EXTENSION, Long.toString(startPeriodTimestamp.getLocalValuePrimitive()));
        extensions.put(END_PERIOD_EXTENSION, Long.toString(endPeriodTimestamp.getLocalValuePrimitive()));
        addExtensions(profileGroupConfig, extensions, measurementFormats);
        return extensions;
    }

    public long getEndTimestamp() {
        return endPeriodTimestamp.getLocalValuePrimitive();
    }

    @SuppressWarnings("rawtypes")
    public void merge(ProfileGroupAcc other) {
        startPeriodTimestamp.merge(other.startPeriodTimestamp);
        endPeriodTimestamp.merge(other.endPeriodTimestamp);
        Iterator<Accumulator<?, ? extends Serializable>> myAccIter = accumulators.iterator();
        Iterator<Accumulator<?, ? extends Serializable>> otherAccIter = other.accumulators.iterator();
        while (myAccIter.hasNext() && otherAccIter.hasNext()) {
            Accumulator myAcc = myAccIter.next();
            Accumulator otherAcc = otherAccIter.next();
            myAcc.merge(otherAcc);
        }
    }

}
