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

package com.cloudera.cyber.caracal;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_PATTERN;
import static com.cloudera.cyber.flink.FlinkUtils.createRawKafkaSource;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;
import static com.cloudera.cyber.parser.ParserJobKafka.PARAMS_CONFIG_TOPIC;
import static com.cloudera.cyber.parser.ParserJobKafka.PARAMS_ORIGINAL_ENABLED;
import static com.cloudera.cyber.parser.ParserJobKafka.PARAMS_ORIGINAL_LOGS_PATH;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.parser.MessageToParse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SerializationSchema;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.util.DigestUtils;

@Slf4j
public class SplitJobKafka extends SplitJob {

    private static final String PARAMS_CONFIG_FILE = "config.file";
    private static final String DEFAULT_CONFIG_FILE = "splits.json";
    private static final String PARAM_COUNT_TOPIC = "count.topic";
    private static final String DEFAULT_COUNT_TOPIC = "parser.counts";

    public SplitJobKafka(String configJson) {
        this.configJson = configJson;
    }

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");

        ParameterTool params = Utils.getParamToolsFromProperties(args);
        // need to load the config file locally and put in a property
        String configJson =
              new String(Files.readAllBytes(Paths.get(params.get(PARAMS_CONFIG_FILE, DEFAULT_CONFIG_FILE))),
                    StandardCharsets.UTF_8);

        log.info(String.format("Splits configuration: %s", configJson));

        StreamExecutionEnvironment env = new SplitJobKafka(configJson)
              .createPipeline(params);
        FlinkUtils.setupEnv(env, params);

        env.execute("Caracal Split Parser");
    }

    @Override
    protected DataStream<SplitConfig> createConfigSource(StreamExecutionEnvironment env, ParameterTool params) {
        String groupId = createGroupId(params.get(PARAMS_TOPIC_INPUT, "") + params.get(PARAMS_TOPIC_PATTERN, ""),
              "cyber-split-parser-config-");
        Properties kafkaProperties = readKafkaProperties(params, groupId, true);

        KafkaSource<String> source =
              KafkaSource.<String>builder().setTopics(params.getRequired(PARAMS_CONFIG_TOPIC))
                         .setValueOnlyDeserializer(new SimpleStringSchema()).setProperties(kafkaProperties).build();

        return env.fromSource(source, WatermarkStrategy.noWatermarks(), "Config Kafka Feed").uid("config.source.kafka")
                  .setParallelism(1).setMaxParallelism(1)
                  .map(new SplitConfigJsonParserMap())
                  .name("Config Source").uid("config.source").setMaxParallelism(1).setParallelism(1);

    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        KafkaSink<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
              params.getRequired(PARAMS_TOPIC_OUTPUT),
              "splits-parser",
              params);
        results.sinkTo(sink).name("Kafka Results").uid("kafka.results");
    }

    @Override
    protected void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results) {
        if (!params.getBoolean(PARAMS_ORIGINAL_ENABLED, true)) {
            return;
        }

        // write the original sources to HDFS files
        Path path = new Path(params.getRequired(PARAMS_ORIGINAL_LOGS_PATH));

        // TODO - add the message id
        // TODO - add filtering (might not care about all raws)
        // TODO - change the factory to support compression
        StreamingFileSink<MessageToParse> sink = StreamingFileSink
              .forBulkFormat(path, ParquetAvroWriters.forReflectRecord(MessageToParse.class))
              .withRollingPolicy(OnCheckpointRollingPolicy.build())
              .withOutputFileConfig(OutputFileConfig
                    .builder()
                    .withPartPrefix("logs")
                    .withPartSuffix(".parquet")
                    .build())
              .build();

        results.addSink(sink).name("Original Archiver").uid("original.archiver");
    }

    @Override
    protected void writeCounts(ParameterTool params, DataStream<Tuple2<String, Long>> sums) {
        Properties kafkaProperties = readKafkaProperties(params, "splits-parser", false);
        String topic = params.get(PARAM_COUNT_TOPIC, DEFAULT_COUNT_TOPIC);

        SerializationSchema<Tuple2<String, Long>> keySerializationSchema =
              new SerializationSchema<Tuple2<String, Long>>() {

                  @Override
                  public void open(InitializationContext context) {

                  }

                  @Override
                  public byte[] serialize(Tuple2<String, Long> metric) {
                      return metric.f0.getBytes();
                  }
              };
        SerializationSchema<Tuple2<String, Long>> valueSerial = new SerializationSchema<Tuple2<String, Long>>() {

            @Override
            public void open(InitializationContext context) {

            }

            @Override
            public byte[] serialize(Tuple2<String, Long> metric) {
                return metric.f1.toString().getBytes();
            }
        };
        KafkaSink<Tuple2<String, Long>> sink = KafkaSink.<Tuple2<String, Long>>builder()
                                                        .setBootstrapServers(kafkaProperties.getProperty(
                                                              ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
                                                        .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                                                                                                           .setTopic(
                                                                                                                 topic)
                                                                                                           .setKeySerializationSchema(
                                                                                                                 keySerializationSchema)
                                                                                                           .setValueSerializationSchema(
                                                                                                                 valueSerial)
                                                                                                           .build())
                                                        .setDeliverGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                                                        .setKafkaProducerConfig(kafkaProperties)
                                                        .build();

        sums.sinkTo(sink).name("Count Results").uid("count.results");
    }

    @Override
    protected DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params,
                                                      Iterable<String> topics) {
        log.info(params.mergeWith(
                             ParameterTool.fromMap(Collections.singletonMap(PARAMS_TOPIC_PATTERN, String.join("|", topics)))).toMap()
                       .toString());
        return createRawKafkaSource(env,
              params.mergeWith(
                    ParameterTool.fromMap(Collections.singletonMap(PARAMS_TOPIC_PATTERN, String.join("|", topics)))),
              createGroupId(params.get("topic.input", "") + params.get("topic.pattern", ""), "cyber-split-parser-"));
    }

    private String createGroupId(String inputTopic, String prefix) {
        return prefix + DigestUtils.md5DigestAsHex(inputTopic.getBytes(StandardCharsets.UTF_8));
    }


}
