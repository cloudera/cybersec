package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.springframework.util.StreamUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@NoArgsConstructor
@RequiredArgsConstructor
@Slf4j
public class EnrichmentBroadcastProcessFunction extends BroadcastProcessFunction<Message, EnrichmentCommand, Message> {
    @NonNull
    private String type;
    @NonNull
    private List<String> fields;
    @NonNull
    private Map<String, MapStateDescriptor<String, Map<String, String>>> broadcastDescriptors;
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
        log.debug("Process Message: {}", message);

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
    public void processBroadcastElement(EnrichmentCommand enrichmentCommand, Context context, Collector<Message> collector) throws Exception {
        // add to the state
        EnrichmentEntry enrichmentEntry = enrichmentCommand.getPayload();
        BroadcastState<String, Map<String, String>> broadcastState = context.getBroadcastState(broadcastDescriptors.get(enrichmentEntry.getType()));
        log.debug("Process Command: {}", enrichmentCommand);
        switch (enrichmentCommand.getType()) {
            case ADD:
                enrichments.inc();
                broadcastState.put(enrichmentEntry.getKey(), enrichmentEntry.getEntries());
                context.output(LookupJob.QUERY_RESULT, EnrichmentCommandResponse.builder()
                        .success(true)
                        .headers(enrichmentCommand.getHeaders())
                        .build());
                break;
            case DELETE:
                broadcastState.remove(enrichmentEntry.getKey());
                context.output(LookupJob.QUERY_RESULT, EnrichmentCommandResponse.builder()
                        .success(true)
                        .headers(enrichmentCommand.getHeaders())
                        .build());
                break;
            case LIST:
                context.output(LookupJob.QUERY_RESULT, EnrichmentCommandResponse.builder()
                        .success(true)
                        .content(StreamSupport.stream(broadcastState.immutableEntries().spliterator(), true)
                                .map(e ->
                                        EnrichmentEntry.builder()
                                                .type(type)
                                                .key(e.getKey())
                                                .entries(e.getValue())
                                                .ts(Instant.now().getEpochSecond())
                                                .build()).collect(Collectors.toList()))
                        .message("")
                        .headers(enrichmentCommand.getHeaders())
                        .build());
                break;
            case FIND:
                context.output(LookupJob.QUERY_RESULT, EnrichmentCommandResponse.builder()
                        .success(true)
                        .content(Arrays.asList(EnrichmentEntry.builder()
                                .type(type)
                                .key(enrichmentEntry.getKey())
                                .entries(broadcastState.get(enrichmentCommand.getPayload().getKey()))
                                .ts(Instant.now().getEpochSecond())
                                .build()))
                        .message("")
                        .headers(enrichmentCommand.getHeaders())
                        .build());
                break;
        }
    }

    private void sendResult(EnrichmentEntry result) {

    }
}
