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

package com.cloudera.cyber.enrichment.threatq;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;
import static com.cloudera.cyber.enrichment.hbase.HbaseJob.PARAMS_ENRICHMENT_CONFIG;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.flink.FlinkUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

@Slf4j
public abstract class ThreatQJob {

    public static DataStream<Message> enrich(DataStream<Message> source,
                                             List<ThreatQConfig> configs,
                                             EnrichmentsConfig enrichmentStorageConfig
    ) {
        return source.process(new ThreatQHBaseMap(configs, enrichmentStorageConfig)).name("Apply ThreatQ")
                     .uid("threatq-enrich");
    }

    public static List<ThreatQConfig> parseConfigs(byte[] configJson) throws IOException {
        return new ObjectMapper().readValue(
              configJson,
              new TypeReference<List<ThreatQConfig>>() {
              });
    }

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);
        DataStream<EnrichmentCommand> enrichmentSource = createEnrichmentSource(env, params);

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));
        List<ThreatQConfig> configs = parseConfigs(configJson);
        log.info("ThreatQ Configs {}", configs);

        EnrichmentsConfig enrichmentsStorageConfig =
              EnrichmentsConfig.load(params.getRequired(PARAMS_ENRICHMENT_CONFIG));

        writeEnrichments(enrichmentSource, enrichmentsStorageConfig, params);

        String enrichmentsStorageConfigFileName = params.get(PARAMS_ENRICHMENT_CONFIG);
        EnrichmentsConfig enrichmentStorageConfig =
              new EnrichmentsConfig(Collections.emptyMap(), Collections.emptyMap());
        if (enrichmentsStorageConfigFileName != null) {
            enrichmentStorageConfig = EnrichmentsConfig.load(enrichmentsStorageConfigFileName);
        }

        DataStream<Message> pipeline = enrich(source, configs, enrichmentStorageConfig);
        writeResults(env, params, pipeline);
        return env;
    }

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params,
                                         DataStream<Message> reduction);

    public abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env,
                                                                            ParameterTool params);

    public abstract void writeEnrichments(DataStream<EnrichmentCommand> enrichmentSource,
                                          EnrichmentsConfig enrichmentsConfig,
                                          ParameterTool params);

}
