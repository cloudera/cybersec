/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.enrichment.lookup;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;
import static com.cloudera.cyber.enrichment.ConfigUtils.allConfigs;
import static com.cloudera.cyber.enrichment.ConfigUtils.enrichmentTypes;
import static com.cloudera.cyber.enrichment.ConfigUtils.typeToFields;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.cloudera.cyber.flink.CyberJob;
import com.cloudera.cyber.flink.FlinkUtils;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;

@Slf4j
public abstract class LookupJob implements CyberJob {

    public static final OutputTag<EnrichmentCommandResponse> QUERY_RESULT =
          new OutputTag<>("query-result", TypeInformation.of(EnrichmentCommandResponse.class));

    public static Tuple2<DataStream<Message>, DataStream<EnrichmentCommandResponse>> enrich(
          DataStream<EnrichmentCommand> baseEnrichmentSource,
          SingleOutputStreamOperator<Message> source,
          List<EnrichmentConfig> configs
    ) {
        DataStream<EnrichmentCommand> enrichmentSource = baseEnrichmentSource.keyBy(e -> EnrichmentKey.builder()
                                                                                                      .type(e.getPayload()
                                                                                                             .getType())
                                                                                                      .key(e.getPayload()
                                                                                                            .getKey())
                                                                                                      .build()
        );

        Map<String, List<String>> typeToFields = typeToFields(configs, EnrichmentKind.LOCAL);
        Set<String> enrichmentTypes = enrichmentTypes(configs, EnrichmentKind.LOCAL);

        Map<String, MapStateDescriptor<String, Map<String, String>>> broadcastDescriptors =
              enrichmentTypes.stream().collect(Collectors.toMap(
                    v -> v,
                    enrichmentType -> new MapStateDescriptor<>(enrichmentType, Types.STRING,
                          Types.MAP(Types.STRING, Types.STRING)))
              );

        Map<String, BroadcastStream<EnrichmentCommand>> enrichmentBroadcasts =
              enrichmentTypes.stream()
                             .map(enrichmentType ->
                                   Tuple2.of(
                                         enrichmentType,
                                         enrichmentSource.filter(
                                                               f -> f.getPayload()
                                                                     .getType()
                                                                     .equals(
                                                                           enrichmentType))
                                                         .name("Filter: "
                                                               + enrichmentType)
                                                         .broadcast(
                                                               broadcastDescriptors.get(
                                                                     enrichmentType)))
                             )
                             .collect(Collectors.toMap(
                                   v -> v.f0,
                                   k -> k.f1));

        /*
         * Apply all the configs as a series of broadcast connections that the messages pass through
         *
         * Note this is done as a reduction to ensure a single pipeline graph is built and a broadcast processor
         * is created for each relevant message key
         */
        SingleOutputStreamOperator<Message> pipeline = source;
        DataStream<EnrichmentCommandResponse> enrichmentCommandResponses = null;
        for (Map.Entry<String, List<String>> enrichmentBroadcast : typeToFields.entrySet()) {
            List<String> fields = enrichmentBroadcast.getValue();
            String type = enrichmentBroadcast.getKey();
            pipeline = pipeline.connect(enrichmentBroadcasts.get(type))
                               .process(new EnrichmentBroadcastProcessFunction(type, fields, broadcastDescriptors))
                               .name("Process: " + type).uid("broadcast-process-" + type);
            if (enrichmentCommandResponses == null) {
                enrichmentCommandResponses = pipeline.getSideOutput(QUERY_RESULT);
            } else {
                enrichmentCommandResponses = enrichmentCommandResponses.union(pipeline.getSideOutput(QUERY_RESULT));
            }
        }

        return Tuple2.of(pipeline, enrichmentCommandResponses);
    }


    @Override
    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        SingleOutputStreamOperator<Message> source = createSource(env, params);
        DataStream<EnrichmentCommand> enrichmentSource = createEnrichmentSource(env, params);

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));

        Tuple2<DataStream<Message>, DataStream<EnrichmentCommandResponse>> pipeline =
              enrich(enrichmentSource, source, allConfigs(configJson));
        writeResults(env, params, pipeline.f0);
        writeQueryResults(env, params, pipeline.f1);
        return env;
    }

    protected abstract void writeQueryResults(StreamExecutionEnvironment env, ParameterTool params,
                                              DataStream<EnrichmentCommandResponse> sideOutput);

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params,
                                         DataStream<Message> reduction);

    public abstract SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env,
                                                                     ParameterTool params);

    protected abstract DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env,
                                                                            ParameterTool params);
}
