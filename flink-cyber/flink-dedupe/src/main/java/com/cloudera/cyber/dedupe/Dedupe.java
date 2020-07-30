package com.cloudera.cyber.dedupe;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.dedupe.impl.CreateKeyFromMessage;
import com.cloudera.cyber.dedupe.impl.EventTimeAndCountTrigger;
import com.cloudera.cyber.dedupe.impl.SumAndMaxTs;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.util.OutputTag;

import java.util.List;
import java.util.Map;

public class Dedupe {
    public static SingleOutputStreamOperator<DedupeMessage> dedupe(DataStream<Message> source, List<String> sessionKey, Long maxTime, Long maxCount, OutputTag<DedupeMessage> lateData, Time allowedLateness) {
        return source
                .map(new CreateKeyFromMessage(sessionKey))
                .keyBy(new KeySelector<DedupeMessage, Map<String, String>>() {
                    @Override
                    public Map<String, String> getKey(DedupeMessage dedupeMessage) throws Exception {
                        return dedupeMessage.getFields();
                    }
                })
                .timeWindow(Time.milliseconds(maxTime))
                .sideOutputLateData(lateData)
                .trigger(EventTimeAndCountTrigger.of(maxCount))
                .aggregate(new SumAndMaxTs());
    }
}
