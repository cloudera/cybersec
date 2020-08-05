package com.cloudera.cyber.enrichment.stix;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.ThreatIntelligence;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Slf4j
public class ThreatIntelligenceBroadcastProcessFunction extends BroadcastProcessFunction<Message, ThreatIntelligence, Message> {

    MapStateDescriptor<String, List<ThreatIntelligence>> descriptor;
    /**
     * A map of message fields to a list of observableTypes
     *
     * Threat intel will be added for each field and each observableTypes for that field.
     */
    Map<String, List<String>> fieldToType;

    @Override
    public void processElement(Message message, ReadOnlyContext readOnlyContext, Collector<Message> collector) throws Exception {
        log.info("processElement %s", message.getId());

        Map<String, List<ThreatIntelligence>> threats = fieldToType.entrySet().stream()
                .collect(Collectors.toMap(f -> f.getKey(), f -> {
                    String value = message.get(f.getKey()).toString();
                    return f.getValue().stream().map(tiType -> tiType + ":" + value)
                            .flatMap(id -> getForKey(readOnlyContext, id))
                            .collect(Collectors.toList());
                }));
        collector.collect(Message.newBuilder(message).setThreats(threats).build());
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
