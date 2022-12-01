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

package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.parser.MessageToParse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.Preconditions;

import java.io.Serializable;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class SplitBroadcastProcessFunction extends KeyedBroadcastProcessFunction<String, MessageToParse, SplitConfig, Message> implements Serializable {

    @NonNull private final Map<String, SplitConfig> configs;
    private Map<String, SplittingFlatMapFunction> splitters = new HashMap<>();
    @NonNull private final PrivateKey signKey;

    public SplitBroadcastProcessFunction(Map<String, SplitConfig> configs, PrivateKey signKey) {
        super();

        Preconditions.checkNotNull(configs, "Must supply split config");
        Preconditions.checkNotNull(signKey, "Must supply signing key");

        this.configs = configs;
        this.signKey = signKey;

        splitters = configs.entrySet().stream().
                collect(Collectors.toMap(
                        k -> k.getKey(),
                        v -> new SplittingFlatMapFunction(v.getValue(), signKey)
                ));
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        splitters.entrySet().stream().forEach(e -> {
            try {
                e.getValue().open(parameters);
            } catch (Exception ex) {
                log.error("Error opening SplitBroadcastProcessFunction", ex);
            }
        });
    }

    @Override
    public void processElement(MessageToParse messageToParse, ReadOnlyContext readOnlyContext, Collector<Message> collector) throws Exception {
        SplittingFlatMapFunction splitter = splitters.get(messageToParse.getTopic());
        if (splitter == null) {
            throw new RuntimeException(String.format("Splitter not found for topic %s", messageToParse.getTopic()));
        }
        splitter.flatMap(messageToParse, collector);
    }

    @Override
    public void processBroadcastElement(SplitConfig splitConfig, Context context, Collector<Message> collector) throws Exception {
        log.info(String.format("Adding splitter %s on thread %d ", splitConfig, Thread.currentThread().getId()));
        context.getBroadcastState(SplitJob.Descriptors.broadcastState).put(splitConfig.getTopic(), splitConfig);
        SplittingFlatMapFunction f = new SplittingFlatMapFunction(splitConfig, signKey);
        f.open(null);
        splitters.put(splitConfig.getTopic(), f);
    }
}