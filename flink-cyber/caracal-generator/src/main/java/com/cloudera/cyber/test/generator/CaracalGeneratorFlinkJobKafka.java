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
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.producer.ProducerRecord;

public class CaracalGeneratorFlinkJobKafka extends CaracalGeneratorFlinkJob {

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = com.cloudera.cyber.flink.Utils.getParamToolsFromProperties(args);
        FlinkUtils.executeEnv(new CaracalGeneratorFlinkJobKafka()
                .createPipeline(params), "Caracal Data generator", params);
    }

    @Override
    protected void writeMetrics(ParameterTool params, SingleOutputStreamOperator<Tuple2<String, Integer>> metrics) {
        FlinkKafkaProducer<Tuple2<String, Integer>> metricsSink = new FlinkKafkaProducer<Tuple2<String, Integer>>(
                "generator.metrics",
                new KafkaSerializationSchema<Tuple2<String, Integer>>() {
                    @Override
                    public ProducerRecord<byte[], byte[]> serialize(Tuple2<String, Integer> stringStringTuple2,
                            @Nullable Long timestamp) {
                        return new ProducerRecord<byte[], byte[]>(
                                params.get("generator.metrics", "generator.metrics"),
                                null,
                                timestamp,
                                stringStringTuple2.f0.getBytes(StandardCharsets.UTF_8),
                                stringStringTuple2.f1.toString().getBytes(StandardCharsets.UTF_8)
                        );
                    }
                },
                Utils.readKafkaProperties(params, false),
                FlinkKafkaProducer.Semantic.NONE);
        metrics.addSink(metricsSink).name("Metrics Sink");
    }

    @Override
    protected void writeResults(ParameterTool params,
            SingleOutputStreamOperator<Tuple2<String, String>> generatedInput) {
        FlinkKafkaProducer<Tuple2<String, String>> kafkaSink = new FlinkKafkaProducer<Tuple2<String, String>>(
                "generator.output",
                new KafkaSerializationSchema<Tuple2<String, String>>() {
                    @Override
                    public ProducerRecord<byte[], byte[]> serialize(Tuple2<String, String> stringStringTuple2,
                            @Nullable Long aLong) {
                        return new ProducerRecord<byte[], byte[]>(
                                stringStringTuple2.f0,
                                stringStringTuple2.f1.getBytes(StandardCharsets.UTF_8)
                        );
                    }
                },
                Utils.readKafkaProperties(params, false),
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE);
        generatedInput.addSink(kafkaSink).name("Text Generator Sink");
    }

    @Override
    protected void writeBinaryResults(ParameterTool params,
                                SingleOutputStreamOperator<Tuple2<String, byte[]>> generatedInput) {
        FlinkKafkaProducer<Tuple2<String, byte[]>> kafkaSink = new FlinkKafkaProducer<Tuple2<String, byte[]>>(
                "generator.binary.output",
                new KafkaSerializationSchema<Tuple2<String, byte[]>>() {
                    @Override
                    public ProducerRecord<byte[], byte[]> serialize(Tuple2<String, byte[]> stringStringTuple2,
                                                                    @Nullable Long aLong) {
                        return new ProducerRecord<byte[], byte[]>(
                                stringStringTuple2.f0,
                                stringStringTuple2.f1
                        );
                    }
                },
                Utils.readKafkaProperties(params, false),
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE);
        generatedInput.addSink(kafkaSink).name("Binary Generator Sink");
    }

}
