package com.cloudera.cyber.scoring;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;

import java.time.Duration;

public class ScoredMessageWatermarkedStream {
    public static DataStream<ScoredMessage> of(DataStream<ScoredMessage> inputStream, long maximumLatenessMillis) {
        WatermarkStrategy<ScoredMessage> watermarkStrategy = WatermarkStrategy
                .<ScoredMessage>forBoundedOutOfOrderness(Duration.ofMillis(maximumLatenessMillis))
                .withTimestampAssigner((scoredMessage, timestamp) -> scoredMessage.getMessage().getTs());
        return inputStream.assignTimestampsAndWatermarks(watermarkStrategy);
    }
}
