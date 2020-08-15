package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.cloudera.cyber.flink.CyberJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.ReadOnlyBroadcastState;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.cloudera.cyber.enrichment.ConfigUtils.*;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

@Slf4j
public abstract class LookupJob implements CyberJob {

    @Override
    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<Message> source = createSource(env, params);
        DataStream<EnrichmentEntry> enrichmentSource = createEnrichmentSource(env, params)
                .keyBy(e -> EnrichmentKey.builder()
                        .type(e.getType())
                        .key(e.getKey())
                        .build()
                );

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));
        Map<String, List<String>> typeToFields = typeToFields(allConfigs(configJson), EnrichmentKind.LOCAL);
        Set<String> enrichmentTypes = enrichmentTypes(allConfigs(configJson), EnrichmentKind.LOCAL);

        Map<String, MapStateDescriptor<String, Map<String, String>>> broadcastDescriptors = enrichmentTypes.stream().collect(Collectors.toMap(
                v -> v,
                enrichmentType -> new MapStateDescriptor<String, Map<String, String>>(enrichmentType, Types.STRING, Types.MAP(Types.STRING, Types.STRING)))
        );
        Map<String, BroadcastStream<EnrichmentEntry>> enrichmentBroadcasts = enrichmentTypes.stream()
                .map(enrichmentType ->
                        Tuple2.of(enrichmentType, enrichmentSource.filter(f -> f.getType().equals(enrichmentType))
                                .broadcast(broadcastDescriptors.get(enrichmentType)))
                )
                .collect(Collectors.toMap(v -> v.f0, k -> k.f1));

        /**
         * Apply all the configs as a series of broadcast connections that the messages pass through
         *
         * Note this is done as a reduction to ensure a single pipeline graph is built and a broadcast processor
         * is created for each relevant message key
         */
        DataStream<Message> pipeline = typeToFields.entrySet().stream().reduce(source,
                (m, entry) -> {
                    List<String> fields = entry.getValue();
                    String type = entry.getKey();
                    return m.connect(enrichmentBroadcasts.get(type))
                            .process(new BroadcastProcessFunction<Message, EnrichmentEntry, Message>() {
                                @Override
                                public void processElement(Message message, ReadOnlyContext readOnlyContext, Collector<Message> collector) throws Exception {
                                    ReadOnlyBroadcastState<String, Map<String, String>> bc = readOnlyContext.getBroadcastState(broadcastDescriptors.get(type));
                                    if (bc != null) {
                                        HashMap<String, String> hm = new HashMap<>();
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
                            }).name("BroadcastProcess: " + type).uid("broadcast-process-" + type);
                }, (a, b) -> a); // TODO - does the combiner really make sense?

        writeResults(env, params, pipeline);
        return env;
    }

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction);

    public abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<EnrichmentEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);

    public static class Descriptors {

        public static MapStateDescriptor<EnrichmentKey, Map<String, String>> broadcast = new MapStateDescriptor<>("enrichment",
                TypeInformation.of(EnrichmentKey.class),
                Types.MAP(Types.STRING, Types.STRING));
    }
}
