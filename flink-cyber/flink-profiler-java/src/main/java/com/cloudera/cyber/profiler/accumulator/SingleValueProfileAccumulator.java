package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.Message;

import java.util.Map;

public abstract class SingleValueProfileAccumulator extends ProfileAccumulator {
    public SingleValueProfileAccumulator(String resultExtensionName) {
        super(resultExtensionName);
    }

    @Override
    public abstract void add(Message message);

    @Override
    public void addResults(Map<String, String> results) {
        results.put(getResultExtensionName(), getResult());
    }

    protected abstract String getResult();

    @Override
    public abstract void merge(ProfileAccumulator other);
}
