package com.cloudera.cyber.profiler.sql.aggregates;

import org.apache.datasketches.hll.HllSketch;
import org.apache.flink.table.functions.FunctionKind;
import org.apache.flink.table.functions.UserDefinedAggregateFunction;

public class Hll extends UserDefinedAggregateFunction<HllSketch, HllSketch> {

    @Override
    public HllSketch createAccumulator() {
        return null;
    }

    @Override
    public FunctionKind getKind() {
        return null;
    }
}
