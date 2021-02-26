package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAcc;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.CoProcessFunction;
import org.apache.flink.util.Collector;

import java.util.Map;
import java.util.stream.Collectors;

public class ProfileStatsJoin extends CoProcessFunction<Message, Message, Message> {
    private transient Map<String, String> currentStats;

    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        currentStats = null;
    }

    @Override
    public void processElement1(Message profileMessage, Context context, Collector<Message> collector) {
        Message messageOut = profileMessage;
        if (currentStats != null) {
            messageOut = MessageUtils.addFields(profileMessage, currentStats);
        }
        collector.collect(messageOut);
    }

    @Override
    public void processElement2(Message statsMessage, Context context, Collector<Message> collector) {
        currentStats = statsMessage.getExtensions().entrySet().stream().filter(e -> isStatsExtension(e.getKey())).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        collector.collect(statsMessage);
    }

    public static boolean isStatsExtension(String extensionName) {
        int suffixStart = extensionName.lastIndexOf('.');
        if (suffixStart > 0) {
            return StatsProfileGroupAcc.STATS_EXTENSION_SUFFIXES.contains(extensionName.substring(suffixStart));
        }
        return false;
    }
}