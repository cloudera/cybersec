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

package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;
import static com.cloudera.cyber.enrichment.ConfigUtils.allConfigs;

public abstract class HbaseJob {

    public static final String PARAMS_ENRICHMENT_CONFIG = "enrichments.config";

    public static DataStream<Message> enrich(DataStream<Message> source, List<EnrichmentConfig> configs, EnrichmentsConfig enrichmentsConfig) {
        return source.process(new HbaseEnrichmentMapFunction(configs, enrichmentsConfig))
                .name("HBase Enrichment Mapper").uid("hbase-map");
    }

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);
        DataStream<EnrichmentCommand> enrichmentSource = createEnrichmentSource(env, params);

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));
        EnrichmentsConfig enrichmentsConfig = EnrichmentsConfig.load(params.getRequired(PARAMS_ENRICHMENT_CONFIG));

        DataStream<Message> result = enrich(source, allConfigs(configJson), enrichmentsConfig);
        writeResults(env, params, result);
        DataStream<EnrichmentCommandResponse> enrichmentCommandResponses = writeEnrichments(env, params, enrichmentSource, enrichmentsConfig);
        writeQueryResults(env, params, enrichmentCommandResponses);

        return env;
    }

    public abstract DataStream<EnrichmentCommandResponse> writeEnrichments(StreamExecutionEnvironment env, ParameterTool params,
                                                                           DataStream<EnrichmentCommand> enrichmentSource,
                                                                           EnrichmentsConfig enrichmentsConfig);

    protected abstract void writeQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> sideOutput);

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> result);

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);
}

