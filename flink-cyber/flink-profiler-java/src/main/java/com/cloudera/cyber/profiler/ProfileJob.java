package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.scoring.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
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
import java.util.concurrent.atomic.AtomicReference;

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
        final DataStream<ScoredMessage> messages = ScoredMessageWatermarkedStream.of(createSource(env, params), allowedLatenessMillis);

        AtomicReference<DataStream<ProfileMessage>> profiledStreams = new AtomicReference<>();
        profileGroups.forEach(g -> profile(params, messages, g, allowedLatenessMillis, profiledStreams));

        DataStream<ScoredMessage> scoredMessages =  score(profiledStreams.get().map(new ProfileMessageToMessageMap()), env, params);
        writeResults(params, scoredMessages);

        return env;
    }

    /**
     * Aggregate events into profile messages for this ProfileGroup.
     *
     * @param params Configuration parameters
     * @param messages Data stream of messages to profile.
     */
    protected void profile(final ParameterTool params, DataStream<ScoredMessage> messages, ProfileGroupConfig profileGroupConfig, long allowedLatenessMillis, AtomicReference<DataStream<ProfileMessage>> profileMessageStreams) {

        Time profilePeriodDuration = Time.of(profileGroupConfig.getPeriodDuration(), TimeUnit.valueOf(profileGroupConfig.getPeriodDurationUnit()));
        DataStream<ProfileMessage> profileMessages = messages.filter(new ProfileMessageFilter(profileGroupConfig)).
                map(new ScoredMessageToProfileMessageMap(profileGroupConfig)).
                keyBy(new MessageKeySelector(profileGroupConfig.getKeyFieldNames())).window(TumblingEventTimeWindows.of(profilePeriodDuration)).
                aggregate(new FieldValueProfileAggregateFunction(profileGroupConfig));
        if (profileGroupConfig.hasFirstSeen()) {
            profileMessages = updateFirstSeen(params, profileMessages, profileGroupConfig);
        }

        if (profileGroupConfig.hasStats()) {
            MessageKeySelector profileKeySelector = new MessageKeySelector(Collections.singletonList(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION));
            Time statsSlide = Time.of(profileGroupConfig.getStatsSlide(), TimeUnit.valueOf(profileGroupConfig.getStatsSlideUnit()));

            DataStream<ProfileMessage> statsStream = ProfileMessage.watermarkedStreamOf(profileMessages, allowedLatenessMillis).
                    keyBy(profileKeySelector).
                    window(SlidingEventTimeWindows.of(profilePeriodDuration, statsSlide)).
                    aggregate(new StatsProfileAggregateFunction(profileGroupConfig));
            StatsProfileKeySelector statsKeySelector = new StatsProfileKeySelector();
            profileMessages = profileMessages.join(statsStream).where(profileKeySelector).equalTo(statsKeySelector).window(TumblingEventTimeWindows.of(profilePeriodDuration)).apply(new ProfileStatsJoin());
            unionProfileMessages(profileMessageStreams, statsStream);
        }

        unionProfileMessages(profileMessageStreams, profileMessages);
    }

    private void unionProfileMessages(AtomicReference<DataStream<ProfileMessage>> profileMessageUnion, DataStream<ProfileMessage> newStream) {
        if (!profileMessageUnion.compareAndSet(null, newStream)) {
            profileMessageUnion.getAndUpdate(union -> union.union(newStream));
        }
    }

    protected abstract DataStream<ScoredMessage> createSource(StreamExecutionEnvironment env, ParameterTool params);
    protected abstract void writeResults(ParameterTool params, DataStream<ScoredMessage> results);
    protected abstract DataStream<ProfileMessage> updateFirstSeen(ParameterTool params, DataStream<ProfileMessage> results, ProfileGroupConfig profileGroupConfig);
    protected abstract DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params);
    protected abstract void writeScoredRuleCommandResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results);

    private DataStream<ScoredMessage> score(DataStream<Message> in, StreamExecutionEnvironment env, ParameterTool params) {
        DataStream<ScoringRuleCommand> rulesSource = createRulesSource(env, params);
        SingleOutputStreamOperator<ScoredMessage> results = ScoringJob.enrich(in, rulesSource);
        writeScoredRuleCommandResult(params, results.getSideOutput(ScoringJob.COMMAND_RESULT_OUTPUT_TAG));
        return results;
    }


}
