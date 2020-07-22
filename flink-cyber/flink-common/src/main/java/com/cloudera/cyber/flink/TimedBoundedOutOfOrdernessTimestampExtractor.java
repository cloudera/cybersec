package com.cloudera.cyber.flink;

import com.cloudera.cyber.Timestamped;
import com.sun.tools.corba.se.idl.constExpr.Times;
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
