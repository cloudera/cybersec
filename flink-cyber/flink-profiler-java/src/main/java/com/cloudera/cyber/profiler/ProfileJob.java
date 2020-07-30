package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.TimedBoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ProfileJob {

/*    private SingleOutputStreamOperator<Message> profile(StreamExecutionEnvironment env, ParameterTool params,  ProfileConfig config) {
        WindowedStream<Message, Object, TimeWindow> messageObjectTimeWindowWindowedStream = createSource(env, params, config.getSource())
                .assignTimestampsAndWatermarks(new MessageTimestampAssigner(Time.milliseconds(config.getLatenessTolerance()))).name("Profile Source").uid("profile-source")
                .filter(new FilterProfiles(config.getFilter())).name("Filter").uid("filter")
                .keyBy(m -> m.get(config.getEntity()))
                .timeWindow(Time.of(config.getInterval(), config.getIntervalUnit()))
                .aggregate(new ProfileAggregateFunction(config.getFields()))
    }*/

    protected abstract Object createSource(String source);

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, String source);

    private void work(StreamExecutionEnvironment env, ParameterTool params) {
        List<ProfileConfig> profileConfigs = new ArrayList<>();

        // group configs by entity and window settings


        Map<ProfileConfigGroup, List<ProfileConfig>> groupedByWindowSpec = profileConfigs.stream().collect(Collectors.groupingBy(ProfileConfigGroup::from));

        groupedByWindowSpec.entrySet().forEach(e -> {

            // source for this window spec and source
            SingleOutputStreamOperator<Message> source = sourceFrom(env, params, e.getKey());

            // for each of the entities, create a filter stream


            // filter by unique filters first
            // TODO - filter hierarchically

            List<SingleOutputStreamOperator<Profile>> profilers = e.getValue().stream().collect(Collectors.groupingBy(ProfileConfig::getFilter)).entrySet().stream()
                    // Add filters to the streams
                    .map(groupedByFilter -> Tuple2.of(
                            groupedByFilter.getValue(),
                            source.filter(new FilterExpressionFunction(groupedByFilter.getKey()))))
                    .flatMap(streamsByConfig ->

                            // for every entity produce a keyed version of the stream
                            streamsByConfig.f0.stream()
                                    .collect(Collectors.groupingBy(ProfileConfig::getEntity))
                                    .entrySet().stream().map(s -> Tuple2.of(streamsByConfig.f0, streamsByConfig.f1.keyBy(s.getKey())))
                    ).flatMap(keyedStreamByConfig ->

                            keyedStreamByConfig.f0.stream().collect(Collectors.groupingBy(ProfileConfig::getFields)).entrySet().stream()
                                    .map(fc -> keyedStreamByConfig.f1.timeWindow(Time.of(e.getKey().getInterval(), e.getKey().getIntervalUnit()))
                                            .allowedLateness(Time.milliseconds(e.getKey().getLatenessTolerance()))
                                            .aggregate(new ProfileAggregateFunction(fc.getKey())))
                    ).collect(Collectors.toList());



        });
    }

    private SingleOutputStreamOperator<Message> sourceFrom(StreamExecutionEnvironment env, ParameterTool params, ProfileConfigGroup config) {
        return createSource(env, params, config.getSource())
                .assignTimestampsAndWatermarks(new TimedBoundedOutOfOrdernessTimestampExtractor<>(Time.milliseconds(config.getLatenessTolerance()))).name("Profile Source").uid("profile-source");
    }
}
