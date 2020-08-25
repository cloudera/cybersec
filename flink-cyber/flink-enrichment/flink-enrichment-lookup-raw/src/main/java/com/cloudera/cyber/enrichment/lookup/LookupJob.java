package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.cloudera.cyber.flink.CyberJob;
import com.cloudera.cyber.flink.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudera.cyber.enrichment.ConfigUtils.*;

@Slf4j
public abstract class LookupJob implements CyberJob {

    public static DataStream<Message> enrich(DataStream<EnrichmentEntry> baseEnrichmentSource,
                                         DataStream<Message> source,
                                         List<EnrichmentConfig> configs
    ) {
        DataStream<EnrichmentEntry> enrichmentSource = baseEnrichmentSource.keyBy(e -> EnrichmentKey.builder()
                .type(e.getType())
                .key(e.getKey())
                .build()
        );

        Map<String, List<String>> typeToFields = typeToFields(configs, EnrichmentKind.LOCAL);
        Set<String> enrichmentTypes = enrichmentTypes(configs, EnrichmentKind.LOCAL);

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
                            .process(new EnrichmentBroadcastProcessFunction(type, fields, broadcastDescriptors)).name("BroadcastProcess: " + type).uid("broadcast-process-" + type);
                }, (a, b) -> a); // TODO - does the combiner really make sense?

        return pipeline;
    }

    @Override
    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);
        DataStream<EnrichmentEntry> enrichmentSource = createEnrichmentSource(env, params);

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));

        DataStream<Message> pipeline = enrich(enrichmentSource, source, allConfigs(configJson));
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
