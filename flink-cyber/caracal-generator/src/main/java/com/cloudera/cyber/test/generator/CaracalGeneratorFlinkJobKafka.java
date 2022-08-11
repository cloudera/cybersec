package com.cloudera.cyber.test.generator;

import com.cloudera.cyber.flink.FlinkUtils;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

public class CaracalGeneratorFlinkJobKafka extends CaracalGeneratorFlinkJob {

    public static void main(String[] args) throws Exception {
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
