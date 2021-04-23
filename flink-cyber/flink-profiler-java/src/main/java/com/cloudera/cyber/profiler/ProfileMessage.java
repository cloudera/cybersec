package com.cloudera.cyber.profiler;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.time.Duration;
import java.util.Map;

@Data
@AllArgsConstructor
public class ProfileMessage {
    private final long ts;
    private final Map<String, String> extensions;

    public static DataStream<ProfileMessage> watermarkedStreamOf(DataStream<ProfileMessage> inputStream, long maximumLatenessMillis) {
        WatermarkStrategy<ProfileMessage> watermarkStrategy = WatermarkStrategy
                .<ProfileMessage>forBoundedOutOfOrderness(Duration.ofMillis(maximumLatenessMillis))
                .withTimestampAssigner((profileMessage, timestamp) -> profileMessage.getTs());
        return inputStream.assignTimestampsAndWatermarks(watermarkStrategy);
    }

}
