package com.cloudera.cyber.profiler.accumulator;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.flink.api.common.accumulators.Accumulator;

import java.util.ArrayList;

public class StatsAcc implements Accumulator<Double, ArrayList<SummaryStatistics>> {

    ArrayList<SummaryStatistics>  mergedStatistics;

    public StatsAcc() {
        resetLocal();
    }

    private StatsAcc(StatsAcc original) {
        mergedStatistics = new ArrayList<>();
        original.mergedStatistics.forEach(s -> mergedStatistics.add(new SummaryStatistics(s)));
    }

    @Override
    public void add(Double doubleValue) {
        mergedStatistics.get(0).addValue(doubleValue);
    }

    @Override
    public ArrayList<SummaryStatistics> getLocalValue() {
        return mergedStatistics;
    }

    @Override
    public void resetLocal() {
        mergedStatistics = new ArrayList<>();
        mergedStatistics.add(new SummaryStatistics());
    }

    @Override
    public void merge(Accumulator<Double, ArrayList<SummaryStatistics>> other) {
        mergedStatistics.addAll(((StatsAcc)other).mergedStatistics);
    }

    @Override
    public Accumulator<Double, ArrayList<SummaryStatistics>> clone() {
        return new StatsAcc(this);
    }
}
