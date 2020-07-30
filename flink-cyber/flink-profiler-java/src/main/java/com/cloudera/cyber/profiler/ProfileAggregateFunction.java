package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.util.Map;

public class ProfileAggregateFunction implements AggregateFunction<Message, ProfileAccumulator, Profile> {
    public ProfileAggregateFunction(Map<String, String> fields) {

    }

    @Override
    public ProfileAccumulator createAccumulator() {
        return new ProfileAccumulator();
    }

    @Override
    public ProfileAccumulator add(Message message, ProfileAccumulator profileAccumulator) {
        return profileAccumulator.add(message);
    }

    @Override
    public Profile getResult(ProfileAccumulator profileAccumulator) {
        return profileAccumulator.getProfile();
    }

    @Override
    public ProfileAccumulator merge(ProfileAccumulator profileAccumulator, ProfileAccumulator acc1) {
        return profileAccumulator.merge(acc1);
    }

}
