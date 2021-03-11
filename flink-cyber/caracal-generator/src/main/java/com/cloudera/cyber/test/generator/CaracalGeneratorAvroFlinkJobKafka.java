package com.cloudera.cyber.test.generator;

import com.cloudera.cyber.flink.FlinkUtils;
import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

public class CaracalGeneratorAvroFlinkJobKafka {

  public static final String PARAMS_RECORDS_LIMIT = "generator.count";
  private static final int DEFAULT_EPS = 0;
  private static final String PARAMS_EPS = "generator.eps";
  private static final String OUTPUT_TOPIC_CONFIG = "topic.output";

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      throw new IllegalArgumentException(
          "Path to the properties file is expected as the only argument.");
    }
    ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
    URI uriSchema = Objects.requireNonNull(CaracalGeneratorAvroFlinkJobKafka.class.getClassLoader()
        .getResource(params.get("generator.schema")))
        .toURI();
    Schema schema = new Parser().parse(new File(uriSchema));
    new CaracalGeneratorAvroFlinkJobKafka().createPipeline(params, schema)
        .execute("Caracal Avro Data generator");
  }


  public StreamExecutionEnvironment createPipeline(ParameterTool params,
      Schema schema) {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    FlinkUtils.setupEnv(env, params);
    env.setStreamTimeCharacteristic(TimeCharacteristic.ProcessingTime);
    Map<GenerationSource, Double> outputs = new HashMap<>();
    outputs.put(new GenerationSource("avro/avro-data.json", params.get(OUTPUT_TOPIC_CONFIG)), 1.0);

    SingleOutputStreamOperator<Tuple2<String, String>> generatedInput =
        env.addSource(
            new FreemarkerTemplateSource(outputs, params.getLong(PARAMS_RECORDS_LIMIT, -1),
                params.getInt(PARAMS_EPS, DEFAULT_EPS))).name("Weighted Data Source");

    // Convert json to generic record
    SingleOutputStreamOperator<GenericRecord> genericRecordInput = generatedInput
        .map(new MapFunction<Tuple2<String, String>, GenericRecord>() {
          @Override
          public GenericRecord map(Tuple2<String, String> tuple2) {
            return Utils.jsonDecodeToAvroGenericRecord(tuple2.f1, schema);
          }
        });
    writeResults(params, genericRecordInput);
    return env;
  }


  protected void writeResults(ParameterTool params,
      SingleOutputStreamOperator<GenericRecord> generatedInput) {
    FlinkKafkaProducer<GenericRecord> kafkaSink = new FlinkUtils<>(GenericRecord.class)
        .createKafkaSink(params.get(OUTPUT_TOPIC_CONFIG), params.get("kafka.group.id"), params);
    generatedInput.addSink(kafkaSink).name("Generator Sink");
  }
}
