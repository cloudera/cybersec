package com.cloudera.cyber.profiler;

import com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAcc;
import org.apache.flink.api.common.functions.JoinFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ProfileStatsJoin implements JoinFunction<ProfileMessage, ProfileMessage, ProfileMessage> {

    public static boolean isStatsExtension(String extensionName) {
        int suffixStart = extensionName.lastIndexOf('.');
        if (suffixStart > 0) {
            return StatsProfileGroupAcc.STATS_EXTENSION_SUFFIXES.contains(extensionName.substring(suffixStart));
        }
        return false;
    }

    @Override
    public ProfileMessage join(ProfileMessage profileMessage, ProfileMessage statsMessage) {
        Map<String, String> statsExtensions = statsMessage.getExtensions().entrySet().stream().filter(e -> isStatsExtension(e.getKey())).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, String> extensionsWithStats = new HashMap<>(profileMessage.getExtensions());
        extensionsWithStats.putAll(statsExtensions);
        return new ProfileMessage(profileMessage.getTs(), extensionsWithStats);
    }
}