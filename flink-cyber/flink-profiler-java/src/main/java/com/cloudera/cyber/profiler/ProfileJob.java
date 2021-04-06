package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class ProfileJob {

    protected static final String PARAM_PROFILE_CONFIG = "profile.config.file";
    protected static final String PARAM_LATENESS_TOLERANCE_MILLIS = "profile.lateness";

    protected static final ObjectMapper jsonObjectMapper =
        new ObjectMapper()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder().
                        allowIfSubType(Map.class).
                        allowIfSubType(List.class).
                        allowIfSubType(java.util.concurrent.TimeUnit.class).
                        build())
                .enable(SerializationFeature.INDENT_OUTPUT);

    protected List<ProfileGroupConfig> parseConfigFile(String configJson) throws JsonProcessingException {
             List<ProfileGroupConfig>  profileGroupConfigs = jsonObjectMapper.readValue(
                configJson,
                new TypeReference<ArrayList<ProfileGroupConfig>>() {
                });
             profileGroupConfigs.forEach(ProfileGroupConfig::verify);
             return profileGroupConfigs;
    }

    protected StreamExecutionEnvironment createPipeline(final ParameterTool params) throws IOException {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        // read the profiler config
        List<ProfileGroupConfig> profileGroups = parseConfigFile(new String(Files.readAllBytes(Paths.get(params.getRequired(PARAM_PROFILE_CONFIG)))));

        long allowedLatenessMillis = params.getLong(PARAM_LATENESS_TOLERANCE_MILLIS, Time.minutes(5).toMilliseconds());
        final DataStream<Message> messages = FlinkUtils.assignTimestamps(createSource(env, params), allowedLatenessMillis);
        profileGroups.forEach(g -> profile(params, messages, g, allowedLatenessMillis));

        return env;
    }

    /**
     * Aggregate events into profile messages for this ProfileGroup.
     *
     * @param params Configuration parameters
     * @param messages Data stream of messages to profile.
     */
    protected void profile(final ParameterTool params, DataStream<Message> messages, ProfileGroupConfig profileGroupConfig, long allowedLatenessMillis) {
        if (profileGroupConfig.needsSourceFilter()) {
            messages = messages.filter(new MessageSourceFilter(profileGroupConfig.getSources()));
        }
        List<String> keyFieldNames = profileGroupConfig.getKeyFieldNames();
        Time profilePeriodDuration = Time.of(profileGroupConfig.getPeriodDuration(), TimeUnit.valueOf(profileGroupConfig.getPeriodDurationUnit()));
        DataStream<Message> profileMessages = messages.filter(new MessageFieldFilter(keyFieldNames)).keyBy(new MessageKeySelector(keyFieldNames)).window(TumblingEventTimeWindows.of(profilePeriodDuration)).
                aggregate(new FieldValueProfileAggregateFunction(profileGroupConfig));
        if (profileGroupConfig.hasFirstSeen()) {
            profileMessages = updateFirstSeen(params, profileMessages, profileGroupConfig);
        }

        if (profileGroupConfig.hasStats()) {
            MessageKeySelector profileKeySelector = new MessageKeySelector(Collections.singletonList(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION));
            profileMessages = FlinkUtils.assignTimestamps(profileMessages, allowedLatenessMillis);

            Time statsSlide = Time.of(profileGroupConfig.getStatsSlide(), TimeUnit.valueOf(profileGroupConfig.getStatsSlideUnit()));

            DataStream<Message> statsStream = profileMessages.
                    keyBy(profileKeySelector).
                    window(SlidingEventTimeWindows.of(profilePeriodDuration, statsSlide)).aggregate(new StatsProfileAggregateFunction(profileGroupConfig));
            StatsProfileKeySelector statsKeySelector = new StatsProfileKeySelector();
            profileMessages = profileMessages.connect(statsStream).keyBy(profileKeySelector, statsKeySelector).process(new ProfileStatsJoin());
        }
        writeResults(params, profileMessages, profileGroupConfig.getProfileGroupName());
    }

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);
    protected abstract void writeResults(ParameterTool params, DataStream<Message> results, String profileGroupName);
    protected abstract DataStream<Message> updateFirstSeen(ParameterTool params, DataStream<Message> results, ProfileGroupConfig profileGroupConfig);

}
