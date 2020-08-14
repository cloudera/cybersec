package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import lombok.AllArgsConstructor;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.co.KeyedBroadcastProcessFunction;
import org.apache.flink.util.Collector;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class EnrichmentBroadcastProcessFunction extends KeyedBroadcastProcessFunction<EnrichmentKey, Message, EnrichmentEntry, Message> {

    private String type;
    private MapStateDescriptor<String, Map<String, String>> descriptor;
    private List<String> fields;


    @Override
    public void processElement(Message message, ReadOnlyContext readOnlyContext, Collector<Message> collector) throws Exception {

        readOnlyContext.getBroadcastState(descriptor);
        // get the current state of the keys for each field


    }

    @Override
    public void processBroadcastElement(EnrichmentEntry enrichmentEntry, Context context, Collector<Message> collector) throws Exception {

    }
}
