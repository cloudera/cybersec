package com.cloudera.cyber.test.generator;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CaracalGeneratorFlinkJob {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new RuntimeException("Path to the properties file is expected as the only argument.");
        }
        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);

        new CaracalGeneratorFlinkJob().run(params);
    }

    public void run(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);

        Map<GenerationSource, Double> outputs = new HashMap<>();
        outputs.put(new GenerationSource("Netflow/netflow_sample_1.json","netflow"), 2.0);
        outputs.put(new GenerationSource("Netflow/netflow_sample_2.json","netflow"), 4.0);
        outputs.put(new GenerationSource("Netflow/netflow_sample_3.json","netflow"), 1.0);

        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_1.json","dpi_http"), 1.5);
        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_2.json","dpi_http"), 1.0);
        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_3.json","dpi_http"), 1.0);
        outputs.put(new GenerationSource("DPI_Logs/Metadata_Module/http/http_sample_4.json","dpi_http"), 1.0);


        SingleOutputStreamOperator<Tuple2<String, String>> generatedInput =
                env.addSource(new FreemarkerTemplateSource(outputs)).name("Weighted Data Source");

        FlinkKafkaProducer<Tuple2<String, String>> kafkaSink = new FlinkKafkaProducer<Tuple2<String, String>>(
                "generator.output",
                new KafkaSerializationSchema<Tuple2<String, String>>() {
                    @Override
                    public ProducerRecord<byte[], byte[]> serialize(Tuple2<String, String> stringStringTuple2, @Nullable Long aLong) {
                        return new ProducerRecord<byte[],byte[]>(
                                stringStringTuple2.f0,
                                stringStringTuple2.f1.getBytes(StandardCharsets.UTF_8)
                        );
                    }
                },
                Utils.readKafkaProperties(params, false),
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE);


        FlinkKafkaProducer<Tuple2<String, Integer>> metricsSink = new FlinkKafkaProducer<Tuple2<String, Integer>>(
                "generator.metrics",
                new KafkaSerializationSchema<Tuple2<String, Integer>>() {
                    @Override
                    public ProducerRecord<byte[], byte[]> serialize(Tuple2<String, Integer> stringStringTuple2, @Nullable Long timestamp) {
                        return new ProducerRecord<byte[],byte[]>(
                                "generator.metrics",
                                null,
                                timestamp,
                                stringStringTuple2.f0.getBytes(StandardCharsets.UTF_8),
                                stringStringTuple2.f1.toString().getBytes(StandardCharsets.UTF_8)
                        );
                    }
                },
                Utils.readKafkaProperties(params, false),
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE);
        

        SingleOutputStreamOperator<Tuple2<String, Integer>> metrics = generatedInput
                .map(new MapFunction<Tuple2<String, String>, Tuple2<String, Integer>>() {
                        @Override
                        public Tuple2<String, Integer> map(Tuple2<String, String> stringStringTuple2) throws Exception {
                            return Tuple2.of(stringStringTuple2.f0, Integer.valueOf(1));
                        }
                    })
                .keyBy(0)
                .window(TumblingProcessingTimeWindows.of(Time.seconds(30)))
                .sum(1);
        metrics.addSink(metricsSink);

        generatedInput.addSink(kafkaSink).name("Kafka Sink");

        env.execute("Caracal Data generator");
    }

}
