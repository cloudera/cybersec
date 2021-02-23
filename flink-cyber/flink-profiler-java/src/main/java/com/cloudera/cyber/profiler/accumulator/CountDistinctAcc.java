package com.cloudera.cyber.profiler.accumulator;

import org.apache.flink.api.common.accumulators.Accumulator;

public class CountDistinctAcc implements Accumulator<String, SerializableUnion> {
    private final SerializableUnion unionWrapper = new SerializableUnion();

    @Override
    public void add(String stringValue) {
        if (stringValue != null) {
            unionWrapper.getUnion().update(stringValue);
        }
    }

    @Override
    public SerializableUnion getLocalValue() {
        return unionWrapper;
    }

    @Override
    public void resetLocal() {
        unionWrapper.getUnion().reset();
    }

    @Override
    public void merge(Accumulator<String, SerializableUnion> other) {
        if (other != null) {
            unionWrapper.getUnion().update(other.getLocalValue().getUnion().getResult());
        }
    }


    @Override
    public Accumulator<String, SerializableUnion> clone() {
        CountDistinctAcc result = new CountDistinctAcc();
        result.unionWrapper.getUnion().update(unionWrapper.getUnion().getResult());
        return result;
    }

    public double getEstimate() {
        return unionWrapper.getUnion().getResult().getEstimate();
    }
}
