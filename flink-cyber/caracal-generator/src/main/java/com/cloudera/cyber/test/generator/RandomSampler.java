package com.cloudera.cyber.test.generator;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.concurrent.ThreadLocalRandom;

public class RandomSampler<T> implements FilterFunction<T> {
    private double threatProbability;
    public RandomSampler(double threatProbability) {
        this.threatProbability = threatProbability;
    }

    @Override
    public boolean filter(T t) throws Exception {
        return ThreadLocalRandom.current().nextDouble(1.0) < threatProbability;
    }
}
