package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.profiler.ProfileGroupConfig;
import com.cloudera.cyber.profiler.ProfileMeasurementConfig;
import lombok.EqualsAndHashCode;

import java.util.*;
import java.util.stream.Collectors;

@EqualsAndHashCode
public class ProfileGroupAccumulator {
    public static final String PROFILE_GROUP_NAME_EXTENSION = "profile";
    public static final String START_PERIOD_EXTENSION = "start_period";
    public static final String END_PERIOD_EXTENSION = "end_period";
    public static final String PROFILE_TOPIC_NAME = "profile";
    public static final String PROFILE_SOURCE = "profile";
    private final String profileGroupName;
    private final List<String> keyFieldNames;
    private long startPeriod = Long.MAX_VALUE;
    private long endPeriod = Long.MIN_VALUE;
    private final List<ProfileAccumulator> measurementAccumulators;
    private Map<String,String> keyFieldValues = Collections.emptyMap();

    public ProfileGroupAccumulator(ProfileGroupConfig config, boolean stats) {
        if (stats) {
            measurementAccumulators = config.getMeasurements().stream().
                    filter(ProfileMeasurementConfig::getCalculateStats).
                    map(ProfileAccumulatorFactory::createStats).collect(Collectors.toList());
            profileGroupName = config.getProfileGroupName().concat(StatsProfileAccumulator.STATS_PROFILE_GROUP_SUFFIX);
            keyFieldNames = Collections.emptyList();
        } else {
            measurementAccumulators = config.getMeasurements().stream().
                    map(ProfileAccumulatorFactory::create).collect(Collectors.toList());
            profileGroupName = config.getProfileGroupName();
            keyFieldNames = config.getKeyFieldNames();
        }
    }

    public ProfileGroupAccumulator add(Message message) {
        if (keyFieldValues.isEmpty()) {
            Map<String, String> extensions = message.getExtensions();
            keyFieldValues = keyFieldNames.stream().
                    collect(Collectors.toMap(fieldName -> fieldName, extensions::get));
        }
        measurementAccumulators.forEach(acc -> acc.add(message));
        updatePeriod(message.getTs(), message.getTs());

        return this;
    }

    public Message getProfileMessage() {
        Map<String, String> extensions = new HashMap<>();
        measurementAccumulators.forEach(a -> a.addResults(extensions));
        extensions.put(PROFILE_GROUP_NAME_EXTENSION, profileGroupName);
        extensions.put(START_PERIOD_EXTENSION, Long.toString(startPeriod));
        extensions.put(END_PERIOD_EXTENSION, Long.toString(endPeriod));
        extensions.putAll(keyFieldValues);
        SignedSourceKey sourceKey  = SignedSourceKey.builder()
                .topic(PROFILE_TOPIC_NAME)
                .partition(0)
                .offset(0)
                .signature(new byte[1])
                .build();
        return Message.builder()
                .extensions(extensions)
                .ts(endPeriod)
                .originalSource(sourceKey)
                .source(PROFILE_SOURCE)
                .build();
    }

    public ProfileGroupAccumulator merge(ProfileGroupAccumulator other) {
        if (other != this && other != null && profileGroupName.equals(other.profileGroupName) &&
                measurementAccumulators.size() == other.measurementAccumulators.size()) {
            Iterator<ProfileAccumulator> myMeasurements = measurementAccumulators.iterator();
            Iterator<ProfileAccumulator> otherMeasurements = other.measurementAccumulators.iterator();
            while(myMeasurements.hasNext() && otherMeasurements.hasNext()) {
                myMeasurements.next().merge(otherMeasurements.next());
            }
            updatePeriod(other.startPeriod, other.endPeriod);
        }
        return this;
    }

    private void updatePeriod(long newStartPeriod, long newEndPeriod) {
        startPeriod = Math.min(startPeriod, newStartPeriod);
        endPeriod = Math.max(endPeriod, newEndPeriod);
    }
}
