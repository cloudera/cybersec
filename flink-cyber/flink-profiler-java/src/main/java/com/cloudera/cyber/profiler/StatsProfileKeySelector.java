package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.functions.KeySelector;

public class StatsProfileKeySelector implements KeySelector<Message, String> {

    @Override
    public String getKey(Message message) {
        String statsProfileName = message.getExtensions().getOrDefault(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION, StatsProfileAggregateFunction.STATS_PROFILE_GROUP_SUFFIX);
        return StringUtils.removeEnd(statsProfileName, StatsProfileAggregateFunction.STATS_PROFILE_GROUP_SUFFIX);
    }
}
