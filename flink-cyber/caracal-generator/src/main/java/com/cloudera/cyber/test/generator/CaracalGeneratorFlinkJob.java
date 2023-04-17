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

package com.cloudera.cyber.test.generator;

import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.generator.FreemarkerTemplateSource;
import com.cloudera.cyber.generator.GenerationSource;
import com.cloudera.cyber.generator.GeneratorConfig;
import com.cloudera.cyber.generator.ThreatGeneratorMap;
import com.cloudera.cyber.libs.networking.IPLocal;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class CaracalGeneratorFlinkJob {

    public static final String PARAMS_RECORDS_LIMIT = "generator.count";
    private static final int DEFAULT_EPS = 0;
    private static final String PARAMS_EPS = "generator.eps";
    protected static final String PARAMS_GENERATOR_CONFIG = "generator.config";
    public static final String PARAMS_SCHEMA = "generator.avro.flag";
    private static final String SCHEMA_PATH = "Netflow/netflow.schema";
    private static final double THREAT_PROBABILITY = 0.01;
    protected static final String NETFLOW_TOPIC = "netflow";
    protected static final String NETFLOW_B_TOPIC = "netflow_b";
    protected static final String DPI_HTTP_TOPIC = "dpi_http";
    protected static final String DPI_DNS_TOPIC = "dpi_dns";
    protected static final String DPI_SMTP_TOPIC = "dpi_smtp";
    protected static final String GENERATOR_AVRO_TOPIC = "generator.avro";
    protected static final String THREAT_TOPIC_NAME = "threats";

    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        SingleOutputStreamOperator<Tuple2<String, byte[]>> generatedInput;
        GeneratorConfig generatorConfig = getGeneratorConfig(params);

        generatedInput = createSourceFromTemplateSource(params, env, generatorConfig);
        generateRandomThreatResults(params, generatedInput, generatorConfig);
        writeMetrics(params, generateMetrics(generatedInput));
        writeResults(params, generatedInput);

        return env;
    }

    private GeneratorConfig getGeneratorConfig(ParameterTool params) throws IOException {
        boolean avroGeneratorFlag = params.getBoolean(PARAMS_SCHEMA, false);
        String generatorConfigFile = params.get(PARAMS_GENERATOR_CONFIG);
        GeneratorConfig generatorConfig = new GeneratorConfig();

        if (avroGeneratorFlag) {
           generatorConfig.setGenerationSources(Collections
                    .singletonList(
                            new GenerationSource("Netflow/netflow_avro_sample1.json", GENERATOR_AVRO_TOPIC, Objects.requireNonNull(getClass().getClassLoader().getResource(SCHEMA_PATH)).toExternalForm(), 1.0)));
        } else if (generatorConfigFile == null) {
            generatorConfig.setGenerationSources(getNetflowSampleMap());
        } else {
            Path configPath = new Path(generatorConfigFile);
            try (InputStream configStream = configPath.getFileSystem().open(configPath)) {
                generatorConfig = new ObjectMapper().readValue(
                        configStream,
                        new TypeReference<GeneratorConfig>() {
                        });
            }
        }
        for(GenerationSource source : generatorConfig.getGenerationSources()) {
            source.readAvroSchema();
        }

        return generatorConfig;
    }

    private SingleOutputStreamOperator<Tuple2<String, Integer>> generateMetrics(
            SingleOutputStreamOperator<Tuple2<String, byte[]>> generatedInput) {
        return generatedInput
                .map(new MapFunction<Tuple2<String, byte[]>, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(Tuple2<String, String> stringStringTuple2) {
                        return Tuple2.of(stringStringTuple2.f0, 1);
                    }
                })
                .keyBy(value -> value.f0)
                .window(TumblingProcessingTimeWindows.of(Time.seconds(30)))
                .sum(1);
    }

    private void generateRandomThreatResults(ParameterTool params,
            SingleOutputStreamOperator<Tuple2<String, byte[]>> generatedInput, GeneratorConfig generatorConfig) {
        // add random threat intelligence for a sample of the generated IPs
        IPLocal localIp = new IPLocal();

        List<String> characterTopics = generatorConfig.getGenerationSources().stream().filter(gs -> Objects.isNull(gs.getOutputAvroSchema())).
                map(GenerationSource::getTopic).collect(Collectors.toList());
        SingleOutputStreamOperator<Tuple2<String, byte[]>> threats = generatedInput.filter(t -> characterTopics.contains(t.f0))
                .map(new GetIpMap())
                .filter(f -> f != null && !localIp.eval(f))
                .filter(new RandomSampler<>(THREAT_PROBABILITY))
                .map(new ThreatGeneratorMap(THREAT_TOPIC_NAME));
        writeResults(params, threats);
    }

    private List<GenerationSource> getNetflowSampleMap() {
        List<GenerationSource> outputs = new ArrayList<>();
        outputs.add(new GenerationSource("Netflow/netflow_sample_1.json", NETFLOW_TOPIC, null, 2.0));
        outputs.add(new GenerationSource("Netflow/netflow_sample_2.json", NETFLOW_TOPIC, null, 4.0));
        outputs.add(new GenerationSource("Netflow/netflow_sample_3.json", NETFLOW_TOPIC, null, 1.0));

        outputs.add(new GenerationSource("Netflow/netflow_sample_b.json", NETFLOW_B_TOPIC, null, 1.0));
        outputs.add(new GenerationSource("Netflow/netflow_sample_b_error.json", NETFLOW_B_TOPIC, null, 1.0));

        outputs.add(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_1.json", DPI_HTTP_TOPIC, null, 1.5));
        outputs.add(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_2.json", DPI_HTTP_TOPIC, null, 1.0));
        outputs.add(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_3.json", DPI_HTTP_TOPIC, null, 1.0));
        outputs.add(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_4.json", DPI_HTTP_TOPIC, null, 1.0));

        outputs.add(new GenerationSource("DPI_Logs/Metadata_Module/DNS/dns_sample_1.json", DPI_DNS_TOPIC, null, 1.0));
        outputs.add(new GenerationSource("DPI_Logs/Metadata_Module/DNS/dns_sample_2.json", DPI_DNS_TOPIC, null, 1.0));
        outputs.add(new GenerationSource("DPI_Logs/Metadata_Module/DNS/dns_sample_3.json", DPI_DNS_TOPIC, null, 1.0));

        outputs.add(new GenerationSource("DPI_Logs/Metadata_Module/SMTP/smtp_sample_1.json", DPI_SMTP_TOPIC, null, 1.0));
        return outputs;
    }

    private SingleOutputStreamOperator<Tuple2<String, byte[]>> createSourceFromTemplateSource(ParameterTool params,
            StreamExecutionEnvironment env, GeneratorConfig outputs) {
        return env.addSource(new FreemarkerTemplateSource(
                outputs, params.getLong(PARAMS_RECORDS_LIMIT, -1), params.getInt(PARAMS_EPS, DEFAULT_EPS)))
                .name("Weighted Data Source");
    }

    protected abstract void writeMetrics(ParameterTool params,
            SingleOutputStreamOperator<Tuple2<String, Integer>> metrics);

    protected abstract void writeResults(ParameterTool params,
            SingleOutputStreamOperator<Tuple2<String, byte[]>> generatedInput);

}
