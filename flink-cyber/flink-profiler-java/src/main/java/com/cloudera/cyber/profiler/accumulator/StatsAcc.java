/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.profiler.accumulator;

import java.util.ArrayList;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.flink.api.common.accumulators.Accumulator;

public class StatsAcc implements Accumulator<Double, ArrayList<SummaryStatistics>> {

    ArrayList<SummaryStatistics> mergedStatistics;

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
        mergedStatistics.addAll(((StatsAcc) other).mergedStatistics);
    }

    @Override
    public Accumulator<Double, ArrayList<SummaryStatistics>> clone() {
        return new StatsAcc(this);
    }
}
