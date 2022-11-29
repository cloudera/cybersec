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

package com.cloudera.cyber.dedupe;

import com.cloudera.cyber.DedupeMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.dedupe.impl.CreateKeyFromMessage;
import com.cloudera.cyber.dedupe.impl.SumAndMaxTs;
import com.cloudera.cyber.flink.EventTimeAndCountTrigger;
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
