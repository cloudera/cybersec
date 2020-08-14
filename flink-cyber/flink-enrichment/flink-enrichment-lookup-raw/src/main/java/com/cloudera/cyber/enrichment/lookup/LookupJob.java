package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.cloudera.cyber.flink.CyberJob;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;

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
                );



        byte[] configJson = Files.readAllBytes(Paths.get("config.json"));
        List<EnrichmentConfig> allConfigs = new ObjectMapper().readValue(
                configJson,
                new TypeReference<List<EnrichmentConfig>>() {
                });

        List<EnrichmentConfig> configs = allConfigs.stream().filter(f -> f.getKind() == EnrichmentKind.LOCAL).collect(Collectors.toList());

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
                ));;

        // broadcast processor for each relevant message key
/*
        configs.stream()
                .filter(c -> c.getKind() == EnrichmentKind.LOCAL)
                .flatMap(c -> c.getFields().stream().map(enrichmentField -> {
                    String type = enrichmentField.getEnrichmentType();
                    String field = enrichmentField.getName();
                    return Tuple2.of(type, field);
                ).collect()
                    return messages
                            .filter(m -> m.getSource().equals(c.getSource()))
                            .connect(enrichmentBroadcasts.get(type)).process(new EnrichmentBroadcastProcessFunction(type, broadcastDescriptors.get(type), fields));
                }));*/

/*        DataStream<EnrichmentLookupResult> r1 = env.fromCollection(Collections.emptyList());
        DataStream<EnrichmentLookupResult> r2 = env.fromCollection(Collections.emptyList());
*/

        return env;
    }

    public abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<EnrichmentEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);

    public static class Descriptors {

        public static MapStateDescriptor<EnrichmentKey, Map<String,String>> broadcast = new MapStateDescriptor<>("enrichment",
                TypeInformation.of(EnrichmentKey.class),
                Types.MAP(Types.STRING, Types.STRING));
    }
}
