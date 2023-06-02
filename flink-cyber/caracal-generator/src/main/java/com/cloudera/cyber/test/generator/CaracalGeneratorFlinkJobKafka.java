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
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.producer.ProducerRecord;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;

import java.nio.charset.StandardCharsets;

import java.nio.charset.StandardCharsets;

public class CaracalGeneratorFlinkJobKafka extends CaracalGeneratorFlinkJob {

    private static final String PRODUCER_ID_PREFIX = "event_generator";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = com.cloudera.cyber.flink.Utils.getParamToolsFromProperties(args);
        FlinkUtils.executeEnv(new CaracalGeneratorFlinkJobKafka()
                .createPipeline(params), "Caracal Data generator", params);
    }

    @Override
    protected void writeMetrics(ParameterTool params, SingleOutputStreamOperator<Tuple2<String, Integer>> metrics) {
        KafkaSink<Tuple2<String, Integer>> metricsSink =  KafkaSink.<Tuple2<String, Integer>>builder().setRecordSerializer(
                (KafkaRecordSerializationSchema<Tuple2<String, Integer>>) (stringIntegerTuple2, kafkaSinkContext, timestamp) -> new ProducerRecord<>(
                        params.get("generator.metrics", "generator.metrics"),
                        null,
                        timestamp,
                        stringIntegerTuple2.f0.getBytes(StandardCharsets.UTF_8),
                        stringIntegerTuple2.f1.toString().getBytes(StandardCharsets.UTF_8)
                )).setKafkaProducerConfig(
                readKafkaProperties(params, PRODUCER_ID_PREFIX.concat("generator.metrics"), false)).setDeliverGuarantee(DeliveryGuarantee.AT_LEAST_ONCE).build();
        metrics.sinkTo(metricsSink).name("Metrics Sink");
    }

    @Override
    protected void writeResults(ParameterTool params,
            SingleOutputStreamOperator<Tuple2<String, byte[]>> generatedInput) {
        KafkaSink<Tuple2<String, byte[]>> kafkaSink = KafkaSink.<Tuple2<String, byte[]>>builder().setRecordSerializer(
                (KafkaRecordSerializationSchema<Tuple2<String, byte[]>>) (stringStringTuple2, kafkaSinkContext, aLong) -> new ProducerRecord<>(
                        stringStringTuple2.f0,
                        stringStringTuple2.f1
                )).
                setKafkaProducerConfig(readKafkaProperties(params, PRODUCER_ID_PREFIX.concat("generator.output"), false)).
                setDeliverGuarantee(DeliveryGuarantee.AT_LEAST_ONCE).
                build();

        generatedInput.sinkTo(kafkaSink).name("Text Generator Sink");
    }

}
