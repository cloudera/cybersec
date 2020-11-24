package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
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
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.cloudera.cyber.enrichment.ConfigUtils.*;

@Slf4j
public abstract class LookupJob implements CyberJob {

    public static final OutputTag<EnrichmentCommandResponse> QUERY_RESULT = new OutputTag<EnrichmentCommandResponse>("query-result", TypeInformation.of(EnrichmentCommandResponse.class));

    public static SingleOutputStreamOperator<Message> enrich(DataStream<EnrichmentCommand> baseEnrichmentSource,
                                                             SingleOutputStreamOperator<Message> source,
                                                             List<EnrichmentConfig> configs
    ) {
        DataStream<EnrichmentCommand> enrichmentSource = baseEnrichmentSource.keyBy(e -> EnrichmentKey.builder()
                .type(e.getPayload().getType())
                .key(e.getPayload().getKey())
                .build()
        );

        Map<String, List<String>> typeToFields = typeToFields(configs, EnrichmentKind.LOCAL);
        Set<String> enrichmentTypes = enrichmentTypes(configs, EnrichmentKind.LOCAL);

        Map<String, MapStateDescriptor<String, Map<String, String>>> broadcastDescriptors = enrichmentTypes.stream().collect(Collectors.toMap(
                v -> v,
                enrichmentType -> new MapStateDescriptor<>(enrichmentType, Types.STRING, Types.MAP(Types.STRING, Types.STRING)))
        );

        Map<String, BroadcastStream<EnrichmentCommand>> enrichmentBroadcasts = enrichmentTypes.stream()
                .map(enrichmentType ->
                        Tuple2.of(enrichmentType, enrichmentSource.filter(f -> f.getPayload().getType().equals(enrichmentType)).name("Filter: " + enrichmentType)
                                .broadcast(broadcastDescriptors.get(enrichmentType)))
                )
                .collect(Collectors.toMap(v -> v.f0, k -> k.f1));

        /**
         * Apply all the configs as a series of broadcast connections that the messages pass through
         *
         * Note this is done as a reduction to ensure a single pipeline graph is built and a broadcast processor
         * is created for each relevant message key
         */
        SingleOutputStreamOperator<Message> pipeline = typeToFields.entrySet().stream().reduce(source,
                (out, entry) -> {
                    List<String> fields = entry.getValue();
                    String type = entry.getKey();
                    return out.connect(enrichmentBroadcasts.get(type))
                            .process(new EnrichmentBroadcastProcessFunction(type, fields, broadcastDescriptors)).name("Process: " + type).uid("broadcast-process-" + type);
                }, (a, b) -> a); // TODO - does the combiner really make sense?
        
        return pipeline;
    }


    @Override
    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        SingleOutputStreamOperator<Message> source = createSource(env, params);
        DataStream<EnrichmentCommand> enrichmentSource = createEnrichmentSource(env, params);

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));

        SingleOutputStreamOperator<Message> pipeline = enrich(enrichmentSource, source, allConfigs(configJson));
        writeResults(env, params, pipeline);
        writeQueryResults(env, params, pipeline.getSideOutput(QUERY_RESULT));
        return env;
    }

    protected abstract void writeQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> sideOutput);

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction);

    public abstract SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);
}
