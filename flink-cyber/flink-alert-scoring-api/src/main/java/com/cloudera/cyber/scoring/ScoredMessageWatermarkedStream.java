/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.scoring;

import java.time.Duration;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;

public class ScoredMessageWatermarkedStream {
    public static DataStream<ScoredMessage> of(DataStream<ScoredMessage> inputStream, long maximumLatenessMillis) {
        WatermarkStrategy<ScoredMessage> watermarkStrategy = WatermarkStrategy
              .<ScoredMessage>forBoundedOutOfOrderness(Duration.ofMillis(maximumLatenessMillis))
              .withTimestampAssigner((scoredMessage, timestamp) -> scoredMessage.getMessage().getTs());
        return inputStream.assignTimestampsAndWatermarks(watermarkStrategy);
    }
}
