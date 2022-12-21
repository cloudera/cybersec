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

package com.cloudera.cyber.enrichment.rest;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cyber.jackson.core.type.TypeReference;
import com.cyber.jackson.databind.ObjectMapper;
import com.cyber.jackson.databind.SerializationFeature;
import com.cyber.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;
import static org.apache.commons.codec.digest.DigestUtils.md5;

@Slf4j
public abstract class RestLookupJob {

    protected static ObjectMapper getConfigObjectMapper() {
        return new ObjectMapper()
            .activateDefaultTyping(BasicPolymorphicTypeValidator.builder().
                allowIfSubType(Map.class).
                allowIfSubType(List.class).
                allowIfBaseType(EndpointAuthorizationConfig.class).
                build())
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static DataStream<Message> enrich(DataStream<Message> source, String enrichmentConfigPath) throws IOException {
        List<RestEnrichmentConfig> restConfig = RestLookupJob.parseConfigs(Files.readAllBytes(Paths.get(enrichmentConfigPath)));
        return enrich(source, restConfig);
    }

    private static DataStream<Message> enrich(DataStream<Message> source, List<RestEnrichmentConfig> configs) {
        return configs.stream().reduce(source, (in, config) -> {
            Preconditions.checkNotNull(config.getSources(),"specify a list of source names or ANY to match any source");
            Preconditions.checkArgument(!config.getSources().isEmpty(), "specify a list of source names or ANY to match any source");
            AsyncHttpRequest asyncHttpRequest = new AsyncHttpRequest(config);
            String processId = "rest-" + md5(source + config.getEndpointTemplate());

            return AsyncDataStream.unorderedWait(
                    in,
                    asyncHttpRequest, config.getTimeoutMillis(), TimeUnit.MILLISECONDS, config.getCapacity())
                    .name("REST - " + config.getEndpointTemplate() + " " + config.getSources())
                    .uid("rest-" + processId);
        }, (a, b) -> a); // TODO - does the combiner really make sense?);
    }

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.enableCheckpointing(5000);
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));

        DataStream<Message> pipeline = enrich(source, parseConfigs(configJson));
        writeResults(env, params, pipeline);
        return env;
    }

    public static List<RestEnrichmentConfig> parseConfigs(byte[] configJson) throws IOException {
        return getConfigObjectMapper().readValue(
                configJson,
                new TypeReference<ArrayList<RestEnrichmentConfig>>() {
                });
    }

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results);

}
