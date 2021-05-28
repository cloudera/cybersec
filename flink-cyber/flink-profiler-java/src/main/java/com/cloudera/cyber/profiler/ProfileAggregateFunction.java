package com.cloudera.cyber.profiler;

import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import lombok.AllArgsConstructor;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.text.DecimalFormat;
import java.util.Map;

@AllArgsConstructor
public abstract class ProfileAggregateFunction implements AggregateFunction<ProfileMessage, ProfileGroupAcc, ProfileMessage> {
    public static final String PROFILE_GROUP_NAME_EXTENSION = "profile";
    public static final String PROFILE_TOPIC_NAME = "profile";
    public static final String PROFILE_SOURCE = "profile";

    protected ProfileGroupConfig profileGroupConfig;
    private final String profileGroupName;
    private final Map<String, DecimalFormat> measurementFormats;

    public ProfileAggregateFunction(ProfileGroupConfig profileGroupConfig, String profileGroupName) {
       this.profileGroupConfig = profileGroupConfig;
       this.profileGroupName = profileGroupName;
       this.measurementFormats = getMeasurementFormats();
    }

    @Override
    public abstract ProfileGroupAcc createAccumulator();

    @Override
    public ProfileGroupAcc add(ProfileMessage message, ProfileGroupAcc profileAccumulator) {
        profileAccumulator.addMessage(message, profileGroupConfig);
        return profileAccumulator;
    }

    @Override
    public ProfileMessage getResult(ProfileGroupAcc profileAccumulator) {

        Map<String, String> extensions = profileAccumulator.getProfileExtensions(profileGroupConfig, measurementFormats);
        extensions.put(PROFILE_GROUP_NAME_EXTENSION, profileGroupName);
        return new ProfileMessage(profileAccumulator.getEndTimestamp(), extensions);
    }

    @Override
    public ProfileGroupAcc merge(ProfileGroupAcc acc1, ProfileGroupAcc acc2) {
         acc1.merge(acc2);
         return acc1;
    }

    protected abstract Map<String, DecimalFormat> getMeasurementFormats();

}
