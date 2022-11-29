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
import com.cloudera.cyber.generator.ThreatGeneratorMap;
import com.cloudera.cyber.libs.networking.IPLocal;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

public abstract class CaracalGeneratorFlinkJob {

    public static final String PARAMS_RECORDS_LIMIT = "generator.count";
    private static final int DEFAULT_EPS = 0;
    private static final String PARAMS_EPS = "generator.eps";
    public static final String PARAMS_SCHEMA = "generator.avro.flag";
    private static final String SCHEMA_PATH = "Netflow/netflow.schema";
    private static final double THREAT_PROBABILITY = 0.01;

    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

        String avroGeneratorFlag = params.get(PARAMS_SCHEMA);
        SingleOutputStreamOperator<Tuple2<String, String>> generatedInput;
        if (BooleanUtils.toBoolean(avroGeneratorFlag)) {
            String schemaString = IOUtils.toString(
                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(SCHEMA_PATH)));
            SingleOutputStreamOperator<Tuple2<String, byte[]>> binaryInput = convertDataToAvro(schemaString, createSourceFromTemplateSource(
                    params, env, Collections
                            .singletonMap(new GenerationSource("Netflow/netflow_avro_sample1.json", "generator.avro"),
                                    1.0)));
            writeBinaryResults(params, binaryInput);
        } else {
            generatedInput = createSourceFromTemplateSource(params, env, getNetflowSampleMap());
            generateRandomThreatResults(params, generatedInput);
            writeMetrics(params, generateMetrics(generatedInput));
            writeResults(params, generatedInput);

        }
        return env;
    }

    private SingleOutputStreamOperator<Tuple2<String, byte[]>> convertDataToAvro(String schemaString,
            SingleOutputStreamOperator<Tuple2<String, String>> generatedInput) {
        return generatedInput
                .map(new AvroMapFunction(schemaString));
    }

    private SingleOutputStreamOperator<Tuple2<String, Integer>> generateMetrics(
            SingleOutputStreamOperator<Tuple2<String, String>> generatedInput) {
        return generatedInput
                .map(new MapFunction<Tuple2<String, String>, Tuple2<String, Integer>>() {
                    @Override
                    public Tuple2<String, Integer> map(Tuple2<String, String> stringStringTuple2) throws Exception {
                        return Tuple2.of(stringStringTuple2.f0, Integer.valueOf(1));
                    }
                })
                .keyBy(0)
                .window(TumblingProcessingTimeWindows.of(Time.seconds(30)))
                .sum(1);
    }

    private void generateRandomThreatResults(ParameterTool params,
            SingleOutputStreamOperator<Tuple2<String, String>> generatedInput) {
        // add random threat intelligence for a sample of the generated IPs
        IPLocal localIp = new IPLocal();

        SingleOutputStreamOperator<Tuple2<String, String>> threats = generatedInput
                .map(new GetIpMap())
                .filter(f -> f != null && !localIp.eval(f))
                .filter(new RandomSampler<>(THREAT_PROBABILITY))
                .map(new ThreatGeneratorMap());
        writeResults(params, threats);
    }

    private Map<GenerationSource, Double> getNetflowSampleMap() {
        Map<GenerationSource, Double> outputs = new HashMap<>();
        outputs.put(new GenerationSource("Netflow/netflow_sample_1.json", "netflow"), 2.0);
        outputs.put(new GenerationSource("Netflow/netflow_sample_2.json", "netflow"), 4.0);
        outputs.put(new GenerationSource("Netflow/netflow_sample_3.json", "netflow"), 1.0);

        outputs.put(new GenerationSource("Netflow/netflow_sample_b.json", "netflow_b"), 1.0);
        outputs.put(new GenerationSource("Netflow/netflow_sample_b_error.json", "netflow_b"), 1.0);

        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_1.json", "dpi_http"), 1.5);
        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_2.json", "dpi_http"), 1.0);
        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_3.json", "dpi_http"), 1.0);
        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_4.json", "dpi_http"), 1.0);

        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/DNS/dns_sample_1.json", "dpi_dns"), 1.0);
        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/DNS/dns_sample_2.json", "dpi_dns"), 1.0);
        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/DNS/dns_sample_3.json", "dpi_dns"), 1.0);

        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/SMTP/smtp_sample_1.json", "dpi_smtp"), 1.0);
        return outputs;
    }

    private SingleOutputStreamOperator<Tuple2<String, String>> createSourceFromTemplateSource(ParameterTool params,
            StreamExecutionEnvironment env, Map<GenerationSource, Double> outputs) {
        return env.addSource(new FreemarkerTemplateSource(
                outputs, params.getLong(PARAMS_RECORDS_LIMIT, -1), params.getInt(PARAMS_EPS, DEFAULT_EPS)))
                .name("Weighted Data Source");
    }

    protected abstract void writeMetrics(ParameterTool params,
            SingleOutputStreamOperator<Tuple2<String, Integer>> metrics);

    protected abstract void writeResults(ParameterTool params,
            SingleOutputStreamOperator<Tuple2<String, String>> generatedInput);

    protected abstract void writeBinaryResults(ParameterTool params,
                                         SingleOutputStreamOperator<Tuple2<String, byte[]>> generatedInput);

}
