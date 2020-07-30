package com.cloudera.cyber.profiler.sql.aggregates;

import org.apache.datasketches.hll.HllSketch;
import org.apache.datasketches.hll.TgtHllType;
import org.apache.datasketches.hll.Union;
import org.apache.flink.table.functions.AggregateFunction;

public class Hll extends AggregateFunction<HllSketch, HllSketch> {

    private static final int lgK = 10;

    @Override
    public HllSketch createAccumulator() {
        return new HllSketch(lgK);
    }

    public void merge(HllSketch accumulator, java.lang.Iterable<HllSketch> its) {
        Union union = new Union(lgK);
        union.update(accumulator);
        for (HllSketch sketch: its) {
            union.update(sketch);
        }

        accumulator = union.getResult(TgtHllType.HLL_8);
        // TODO - figure out how to replace the current accumulator with this output

    }

    public void accumulate(HllSketch accumulator, String datum) {
        accumulator.update(datum);
    }

    @Override
    public HllSketch getValue(HllSketch hllSketch) {
        return hllSketch;
    }

    public void resetAccumulator(HllSketch accumulator) {
       accumulator.reset();
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }
}
