package com.cloudera.cyber.flink;

import com.cloudera.cyber.Timestamped;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;

public class TimedBoundedOutOfOrdernessTimestampExtractor<T extends Timestamped> extends BoundedOutOfOrdernessTimestampExtractor<T> {

    public TimedBoundedOutOfOrdernessTimestampExtractor(Time lateness) {
        super(lateness);
    }

    @Override
    public long extractTimestamp(T ts) {
        return ts.getTs();
    }
}
