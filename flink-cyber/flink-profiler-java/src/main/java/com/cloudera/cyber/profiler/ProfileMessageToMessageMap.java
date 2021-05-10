package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import org.apache.flink.api.common.functions.MapFunction;

import static com.cloudera.cyber.profiler.ProfileAggregateFunction.PROFILE_SOURCE;
import static com.cloudera.cyber.profiler.ProfileAggregateFunction.PROFILE_TOPIC_NAME;

public class ProfileMessageToMessageMap implements MapFunction<ProfileMessage, Message> {

    @Override
    public Message map(ProfileMessage profileMessage) throws Exception {

        SignedSourceKey sourceKey  = SignedSourceKey.builder()
                .topic(PROFILE_TOPIC_NAME)
                .partition(0)
                .offset(0)
                .signature(new byte[1])
                .build();
        return Message.builder()
                .extensions(profileMessage.getExtensions())
                .ts(profileMessage.getTs())
                .originalSource(sourceKey)
                .source(PROFILE_SOURCE)
                .build();
    }
}
