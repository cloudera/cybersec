package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAccumulator;
import lombok.AllArgsConstructor;
import org.apache.flink.api.common.functions.AggregateFunction;

@AllArgsConstructor
public class ProfileAggregateFunction implements AggregateFunction<Message, ProfileGroupAccumulator, Message> {
    private ProfileGroupConfig profileGroupConfig;
    private boolean stats;

    @Override
    public ProfileGroupAccumulator createAccumulator() {
        return new ProfileGroupAccumulator(profileGroupConfig, stats);
    }

    @Override
    public ProfileGroupAccumulator add(Message message, ProfileGroupAccumulator profileAccumulator) {
        return profileAccumulator.add(message);
    }

    @Override
    public Message getResult(ProfileGroupAccumulator profileAccumulator) {
        return profileAccumulator.getProfileMessage();
    }

    @Override
    public ProfileGroupAccumulator merge(ProfileGroupAccumulator profileAccumulator, ProfileGroupAccumulator acc1) {
        return profileAccumulator.merge(acc1);
    }

}
