package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import lombok.AllArgsConstructor;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.text.DecimalFormat;
import java.util.Map;

@AllArgsConstructor
public abstract class ProfileAggregateFunction implements AggregateFunction<Message, ProfileGroupAcc, Message> {
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
    public ProfileGroupAcc add(Message message, ProfileGroupAcc profileAccumulator) {
        profileAccumulator.addMessage(message, profileGroupConfig);
        return profileAccumulator;
    }

    @Override
    public Message getResult(ProfileGroupAcc profileAccumulator) {

        Map<String, String> extensions = profileAccumulator.getProfileExtensions(profileGroupConfig, measurementFormats);
        extensions.put(PROFILE_GROUP_NAME_EXTENSION, profileGroupName);
        SignedSourceKey sourceKey  = SignedSourceKey.builder()
                .topic(PROFILE_TOPIC_NAME)
                .partition(0)
                .offset(0)
                .signature(new byte[1])
                .build();
        return Message.builder()
                .extensions(extensions)
                .ts(profileAccumulator.getEndTimestamp())
                .originalSource(sourceKey)
                .source(PROFILE_SOURCE)
                .build();
    }

    @Override
    public ProfileGroupAcc merge(ProfileGroupAcc acc1, ProfileGroupAcc acc2) {
         acc1.merge(acc2);
         return acc1;
    }

    protected abstract Map<String, DecimalFormat> getMeasurementFormats();

}
