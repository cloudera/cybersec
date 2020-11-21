package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class EnrichmentBroadcastProcessFunction extends BroadcastProcessFunction<Message, EnrichmentEntry, Message> {
    @NonNull private String type;
    @NonNull private List<String> fields;
    @NonNull private Map<String, MapStateDescriptor<String, Map<String, String>>> broadcastDescriptors;
    private transient Counter enrichments;
    private transient Counter hits;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.enrichments = getRuntimeContext().getMetricGroup().counter("enrichments");
        this.hits = getRuntimeContext().getMetricGroup().counter("hits");
    }

    @Override
    public void processElement(Message message, ReadOnlyContext readOnlyContext, Collector<Message> collector) throws Exception {
        ReadOnlyBroadcastState<String, Map<String, String>> bc = readOnlyContext.getBroadcastState(broadcastDescriptors.get(type));
        if (bc != null) {
            HashMap<String, String> hm = new HashMap<>();
            for (String field : fields) {
                Object value = message.getExtensions().get(field);
                if (value != null && bc.contains(value.toString())) {
                    hits.inc();
                    hm.putAll(bc.get(value.toString()).entrySet().stream().collect(Collectors.toMap(
                            k -> field + "_" + k.getKey(),
                            v -> v.getValue()
                    )));
                }
            }
            collector.collect(MessageUtils.addFields(message, hm));
        } else {
            log.debug("No broadcast lookup for %s, passthrough message", type);
            collector.collect(message);
        }
    }

    @Override
    public void processBroadcastElement(EnrichmentEntry enrichmentEntry, Context context, Collector<Message> collector) throws Exception {
        // add to the state
        enrichments.inc();
        context.getBroadcastState(broadcastDescriptors.get(enrichmentEntry.getType()))
                .put(
                        enrichmentEntry.getKey(),
                        enrichmentEntry.getEntries()
                );
    }
}
