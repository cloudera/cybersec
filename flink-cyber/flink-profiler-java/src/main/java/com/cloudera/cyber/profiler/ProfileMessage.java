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

package com.cloudera.cyber.profiler;

import java.time.Duration;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;

@Data
@AllArgsConstructor
public class ProfileMessage {
    private final long ts;
    private final Map<String, String> extensions;

    public static DataStream<ProfileMessage> watermarkedStreamOf(DataStream<ProfileMessage> inputStream,
                                                                 long maximumLatenessMillis) {
        WatermarkStrategy<ProfileMessage> watermarkStrategy = WatermarkStrategy
              .<ProfileMessage>forBoundedOutOfOrderness(Duration.ofMillis(maximumLatenessMillis))
              .withTimestampAssigner((profileMessage, timestamp) -> profileMessage.getTs());
        return inputStream.assignTimestampsAndWatermarks(watermarkStrategy);
    }

}
