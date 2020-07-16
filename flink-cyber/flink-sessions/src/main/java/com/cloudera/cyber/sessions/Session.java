package com.cloudera.cyber.sessions;

import com.cloudera.cyber.GroupedMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.operators.MessageConcatenate;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Session {
    public static SingleOutputStreamOperator<GroupedMessage> sessionize(DataStream<Message> source, final List<String> sessionKey, final Long sessionTimeout) {
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

}
