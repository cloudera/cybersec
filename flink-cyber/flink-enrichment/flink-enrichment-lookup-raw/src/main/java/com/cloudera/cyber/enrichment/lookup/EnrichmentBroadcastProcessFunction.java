package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class EnrichmentBroadcastProcessFunction extends BroadcastProcessFunction<Message, EnrichmentEntry, Message> {
    String type;
    List<String> fields;
    Map<String, MapStateDescriptor<String, Map<String, String>>> broadcastDescriptors;

    @Override
    public void processElement(Message message, ReadOnlyContext readOnlyContext, Collector<Message> collector) throws Exception {
        ReadOnlyBroadcastState<String, Map<String, String>> bc = readOnlyContext.getBroadcastState(broadcastDescriptors.get(type));
        if (bc != null) {
            HashMap<String, Object> hm = new HashMap<>();
            for (String field : fields) {
                Object value = message.getExtensions().get(field);
                if (value != null && bc.contains(value.toString())) {
                    hm.putAll(bc.get(value.toString()).entrySet().stream().collect(Collectors.toMap(
                            k -> field + "_" + k.getKey(),
                            v -> v.getValue()
                    )));
                }
            }
            collector.collect(MessageUtils.addFields(message, hm));
        } else {
            log.warn("Failed to find broadcast lookup for %s", type);
        }
    }

    @Override
    public void processBroadcastElement(EnrichmentEntry enrichmentEntry, Context context, Collector<Message> collector) throws Exception {
        // add to the state
        context.getBroadcastState(broadcastDescriptors.get(enrichmentEntry.getType()))
                .put(
                        enrichmentEntry.getKey(),
                        enrichmentEntry.getEntries()
                );
    }
}
