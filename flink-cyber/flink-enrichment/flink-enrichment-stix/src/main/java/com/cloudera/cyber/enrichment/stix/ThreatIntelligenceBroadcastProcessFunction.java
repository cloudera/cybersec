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

package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.ThreatIntelligence;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public class ThreatIntelligenceBroadcastProcessFunction extends BroadcastProcessFunction<Message, ThreatIntelligence, Message> {
    @NonNull MapStateDescriptor<String, List<ThreatIntelligence>> descriptor;
    /**
     * A map of message fields to a list of observableTypes
     *
     * Threat intel will be added for each field and each observableTypes for that field.
     */
    @NonNull Map<String, List<String>> fieldToType;
    private transient Counter hits;
    private transient Counter hitMessages;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.hits = getRuntimeContext().getMetricGroup().counter("hits");
        this.hitMessages = getRuntimeContext().getMetricGroup().counter("hitMessages");
    }

    @Override
    public void processElement(Message message, ReadOnlyContext readOnlyContext, Collector<Message> collector) throws Exception {
        log.debug("processElement {}", message.getId());

        Map<String, List<ThreatIntelligence>> threats = fieldToType.entrySet().stream()
                .collect(Collectors.toMap(f -> f.getKey(), f -> {
                    String value = message.getExtensions().get(f.getKey());
                    return f.getValue().stream().map(tiType -> tiType + ":" + value)
                            .flatMap(id -> getForKey(readOnlyContext, id))
                            .collect(Collectors.toList());
                }));
        if (threats.size() > 0) {
            this.hitMessages.inc();
            this.hits.inc(threats.size());
            collector.collect(message.toBuilder().threats(threats).build());
        } else {
            collector.collect(message);
        }
    }

    /**
     * TODO - should we store a bloomfilter instead of actual threats?
     *
     * @param threatIntelligence
     * @param context
     * @param collector
     * @throws Exception
     */
    @Override
    public void processBroadcastElement(ThreatIntelligence threatIntelligence, Context context, Collector<Message> collector) throws Exception {
        BroadcastState<String, List<ThreatIntelligence>> state = context.getBroadcastState(descriptor);

        String tiKey = threatIntelligence.getObservableType() + ":" + threatIntelligence.getObservable();
        List<ThreatIntelligence> oldThreatIntelligences = state.get(tiKey);

        log.info(String.format("processBroadcastElement [%d], %s, tiKey: %s, oldCount: %d", Thread.currentThread().getId(), threatIntelligence.toString(), tiKey, oldThreatIntelligences == null ? -1: oldThreatIntelligences.size()));
        if (oldThreatIntelligences != null) {
            Set<ThreatIntelligence> threatIntelligences = new HashSet(oldThreatIntelligences);
            threatIntelligences.add(threatIntelligence);
            state.put(tiKey, new ArrayList(threatIntelligences));
        } else {
            state.put(tiKey, Arrays.asList(threatIntelligence));
        }
    }

    private Stream<ThreatIntelligence> getForKey(ReadOnlyContext readOnlyContext, String tiKey) {
        try {
            return readOnlyContext.getBroadcastState(descriptor).get(tiKey).stream();
        } catch (Exception e) {
            return Stream.empty();
        }
    }

}
