package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.cloudera.cyber.flink.CyberJob;
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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

public abstract class LookupJob implements CyberJob {

    protected static final String PARAMS_CONFIG_FILE = "config.file";

    @Override
    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<Message> source = createSource(env, params);
        DataStream<EnrichmentEntry> enrichmentSource = createEnrichmentSource(env, params)
                .keyBy(e -> EnrichmentKey.builder()
                        .type(e.getType())
                        .key(e.getKey())
                );

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));
        List<EnrichmentConfig> allConfigs = new ObjectMapper().readValue(
                configJson,
                new TypeReference<List<EnrichmentConfig>>() {
                });

        List<EnrichmentConfig> configs = allConfigs.stream()
                .filter(f -> f.getKind() == EnrichmentKind.LOCAL)
                .collect(Collectors.toList());

        // store the enrichments in a keyed broadcast state

        // make a broadcast for each enrichmentType

        Set<String> enrichmentTypes = configs.stream().flatMap(s -> s.getFields().stream().map(m -> m.getEnrichmentType())).collect(Collectors.toSet());

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

        DataStream<Message> messages = createSource(env, params);


        Map<String, List<String>> typeToFields = configs.stream()
                .filter(c -> c.getKind() == EnrichmentKind.LOCAL)
                .flatMap(c -> c.getFields().stream()
                        .collect(groupingBy(EnrichmentField::getEnrichmentType,
                                mapping(EnrichmentField::getName, Collectors.toList())))
                        .entrySet().stream()
                )
                .collect(Collectors.toMap(k -> k.getKey(), v -> v.getValue(), (a, b) ->
                        Stream.of(a, b)
                                .flatMap(x -> x.stream())
                                .collect(Collectors.toList())
                ));
        ;

        //

        /**
         * Apply all the configs as a series of broadcast connections that the messages pass through
         *
         * Note this is done as a reduction to ensure a single pipeline graph is built and a broadcast processor
         * is created for each relevant message key
         */
        DataStream<Message> pipeline = typeToFields.entrySet().stream().reduce(messages,
                (m, entry) -> {
                    List<String> fields = entry.getValue();
                    String type = entry.getKey();
                    return m.connect(enrichmentBroadcasts.get(type))
                            .process(new BroadcastProcessFunction<Message, EnrichmentEntry, Message>() {
                                @Override
                                public void processElement(Message message, ReadOnlyContext readOnlyContext, Collector<Message> collector) throws Exception {
                                    ReadOnlyBroadcastState<String, Map<String, String>> bc = readOnlyContext.getBroadcastState(broadcastDescriptors.get(type));

                                    HashMap<String, String> hm = new HashMap<>();
                                    for (String field : fields) {
                                        hm.putAll(bc.get(field).entrySet().stream().collect(Collectors.toMap(
                                                k -> field + "_" + k.getKey(),
                                                v -> v.getValue()
                                        )));
                                    }

                                    collector.collect(MessageUtils.addFields(message, hm));
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
                            });
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
