package com.cloudera.cyber.sessions;

import com.cloudera.cyber.GroupedMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.operators.MessageConcatenate;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class SessionJob {
    private static final long DEFAULT_SESSION_TIMEOUT = 1000;
    protected static final String PARAM_SESSION_KEY = "session.key";
    protected static final String PARAM_SESSION_TIMEOUT = "session.timeout";
    protected static final String PARAM_SESSION_LIMIT = "session.limit";

    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        List<String> sessionKey = Arrays.asList(params.get(PARAM_SESSION_KEY).split(","));
        Long sessionTimeout = params.getLong(PARAM_SESSION_TIMEOUT, DEFAULT_SESSION_TIMEOUT);

        DataStream<Message> source = createSource(env, params, sessionKey, sessionTimeout);
        SingleOutputStreamOperator<GroupedMessage> results = sessionize(source, sessionKey, sessionTimeout);
        writeResults(params, results);

        return env;
    }

    private SingleOutputStreamOperator<GroupedMessage> sessionize(DataStream<Message> source, final List<String> sessionKey, final Long sessionTimeout) {
        return source
                .keyBy(new KeySelector<Message, Map<String, String>>() {
                    @Override
                    public Map<String, String> getKey(Message message) throws Exception {
                        return sessionKey.stream().collect(Collectors.toMap(
                                f -> f.toString(),
                                f -> message.get(f).toString()));
                    }
                })
                .window(EventTimeSessionWindows.withGap(Time.milliseconds(sessionTimeout)))
                .aggregate(new MessageConcatenate());
    }

    protected abstract void writeResults(ParameterTool params, SingleOutputStreamOperator<GroupedMessage> results);
    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> sessionKey, Long sessionTimeout);

}