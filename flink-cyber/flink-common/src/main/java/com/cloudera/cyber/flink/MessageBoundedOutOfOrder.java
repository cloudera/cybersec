package com.cloudera.cyber.flink;

import com.cloudera.cyber.Message;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;

public class MessageBoundedOutOfOrder extends BoundedOutOfOrdernessTimestampExtractor<Message> {
    public MessageBoundedOutOfOrder(Time maxOutOfOrderness) {
        super(maxOutOfOrderness);
    }

    @Override
    public long extractTimestamp(Message message) {
        return message.getTs().getMillis();
    }
}

