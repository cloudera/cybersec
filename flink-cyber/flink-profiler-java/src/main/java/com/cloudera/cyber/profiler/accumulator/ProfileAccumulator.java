package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.Message;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Map;


@ToString
@EqualsAndHashCode
public abstract class ProfileAccumulator {
    private String resultExtensionName;

    public ProfileAccumulator(String resultExtensionName) {
        this.resultExtensionName = resultExtensionName;
    }

    public String getResultExtensionName() {
        return resultExtensionName;
    }

    public abstract void add(Message message);

    public abstract void addResults(Map<String, String> results);

    public abstract void merge(ProfileAccumulator acc1);

}

