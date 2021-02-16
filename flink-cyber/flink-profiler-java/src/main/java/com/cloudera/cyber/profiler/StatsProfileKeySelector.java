package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAccumulator;
import com.cloudera.cyber.profiler.accumulator.StatsProfileAccumulator;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.functions.KeySelector;

public class StatsProfileKeySelector implements KeySelector<Message, String> {

    @Override
    public String getKey(Message message) {
        String statsProfileName = message.getExtensions().getOrDefault(ProfileGroupAccumulator.PROFILE_GROUP_NAME_EXTENSION, StatsProfileAccumulator.STATS_PROFILE_GROUP_SUFFIX);
        return StringUtils.removeEnd(statsProfileName, StatsProfileAccumulator.STATS_PROFILE_GROUP_SUFFIX);
    }
}
