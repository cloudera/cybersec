package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.SingleValueEnrichment;
import org.apache.datasketches.theta.SetOperation;
import org.apache.datasketches.theta.Union;

import java.text.DecimalFormat;
import java.util.Map;


public class CountDistinctProfileAccumulator extends SingleValueProfileAccumulator {

    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("#");
    private String fieldName;
    private DecimalFormat format;
    private final Union union = SetOperation.builder().buildUnion();

    public CountDistinctProfileAccumulator(String resultExtensionName, String fieldName, DecimalFormat format) {
        super(resultExtensionName);
        this.fieldName = fieldName;
        this.format = format != null ? format : DEFAULT_FORMAT;
    }

    @Override
    public void add(Message message) {
        String fieldValue = message.getExtensions().get(fieldName);
        if (fieldValue != null) {
            union.update(fieldValue);
        }
    }

    @Override
    public String getResult() {
        return format.format(union.getResult().getEstimate());
    }

    @Override
    public void merge(ProfileAccumulator other) {
        if (this != other && other instanceof CountDistinctProfileAccumulator) {
            union.update(((CountDistinctProfileAccumulator)other).union.getResult());
        }
    }

    public String getFieldName() {
        return fieldName;
    }
}
