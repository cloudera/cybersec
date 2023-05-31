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

package com.cloudera.cyber.flink;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.cyber.parser.MessageToParseDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.KafkaSourceBuilder;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.formats.registry.cloudera.avro.ClouderaRegistryAvroKafkaDeserializationSchema;
import org.apache.flink.formats.registry.cloudera.avro.ClouderaRegistryAvroKafkaRecordSerializationSchema;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;

import java.util.Properties;
import java.util.regex.Pattern;

import static com.cloudera.cyber.flink.Utils.readKafkaProperties;
import static com.cloudera.cyber.flink.Utils.readSchemaRegistryProperties;

@Slf4j
public class FlinkUtils<T> {

    private static final String PARAMS_CHECKPOINT_INTERVAL = "checkpoint.interval.ms";
    private static final int DEFAULT_CHECKPOINT_INTERVAL = 60000;
    public static final String PARAMS_PARALLELISM = "parallelism";
    private static final int DEFAULT_PARALLELISM = 2;

    private final Class<T> type;

    public FlinkUtils(Class<T> type) {
        this.type = type;
    }

    public static void setupEnv(StreamExecutionEnvironment env, ParameterTool params) {
        env.enableCheckpointing(params.getInt(PARAMS_CHECKPOINT_INTERVAL, DEFAULT_CHECKPOINT_INTERVAL), CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(params.getInt(PARAMS_PARALLELISM, DEFAULT_PARALLELISM));
        env.getConfig().setGlobalJobParameters(params);
    }

    public static void executeEnv(StreamExecutionEnvironment env,  String defaultJobName, ParameterTool params) throws Exception {
        env.execute(params.get("flink.job.name",defaultJobName));
    }

    public static KafkaSource<String> createKafkaStringSource(String topic, Properties kafkaProperties) {
        return KafkaSource.<String>builder().setBootstrapServers(kafkaProperties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)).setTopics(topic).setValueOnlyDeserializer(new SimpleStringSchema()).setProperties(kafkaProperties).build();
    }

    public KafkaSink<T> createKafkaSink(final String topic, String groupId, final ParameterTool params) {
        Preconditions.checkNotNull(topic, "Must specific output topic");

        Properties kafkaProperties = readKafkaProperties(params, groupId, false);
        log.info("Creating Kafka Sink for {}, using {}", topic, kafkaProperties);

        KafkaRecordSerializationSchema<T> schema = ClouderaRegistryAvroKafkaRecordSerializationSchema
                .<T>builder(topic)
                .setConfig(readSchemaRegistryProperties(params))
                .build();

        return KafkaSink.<T>builder()
                .setBootstrapServers(kafkaProperties.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG))
                .setRecordSerializer(schema)
                .setKafkaProducerConfig(kafkaProperties)
                .build();
    }

    public KafkaSource<T> createKafkaGenericSource(String topic, ParameterTool params, String groupId) {
        Preconditions.checkNotNull(topic, "Must specific input topic");
        Preconditions.checkNotNull(groupId, "Must specific group id");

        Properties kafkaProperties = readKafkaProperties(params, groupId, true);
        log.info(String.format("Creating Kafka Source for %s, using %s", topic, kafkaProperties));
        KafkaDeserializationSchema<T> schema = ClouderaRegistryAvroKafkaDeserializationSchema
                .builder(type)
                .setConfig(readSchemaRegistryProperties(params))
                .build();

        return KafkaSource.<T>builder().setTopics(topic).
                setBootstrapServers(kafkaProperties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)).
                setProperties(kafkaProperties).
                setDeserializer(KafkaRecordDeserializationSchema.of(schema)).
                build();
    }

    private KafkaSourceBuilder<T> createKafkaSourceBuilder(ParameterTool params, String groupId) {
        Preconditions.checkNotNull(groupId, "Must specific group id");

        Properties kafkaProperties = readKafkaProperties(params, groupId, true);
        KafkaDeserializationSchema<T> schema = ClouderaRegistryAvroKafkaDeserializationSchema
                .builder(type)
                .setConfig(readSchemaRegistryProperties(params))
                .build();

        return KafkaSource.<T>builder().
                setBootstrapServers(kafkaProperties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)).
                setProperties(kafkaProperties).
                setDeserializer(KafkaRecordDeserializationSchema.of(schema));

    }
    public static KafkaSource<Message> createKafkaSource(String topic, ParameterTool params, String groupId) {
        Preconditions.checkNotNull(topic, "Must specific input topic");

       return  new FlinkUtils<>(Message.class).createKafkaSourceBuilder(params, groupId).setTopics(topic).build();
    }

    public static KafkaSource<Message> createKafkaSource(Pattern topic, ParameterTool params, String groupId) {
        Preconditions.checkNotNull(topic, "Must specific input topic pattern");

        return  new FlinkUtils<>(Message.class).createKafkaSourceBuilder(params, groupId).setTopicPattern(topic).build();
    }

    public static <T> DataStream<T> createRawKafkaSource(StreamExecutionEnvironment env, ParameterTool params, String groupId, KafkaRecordDeserializationSchema<T> deserializationSchema) {
        String inputTopic = params.get(ConfigConstants.PARAMS_TOPIC_INPUT,"");
        String pattern = params.get(ConfigConstants.PARAMS_TOPIC_PATTERN, "");

        log.info(String.format("createRawKafkaSource topic: '%s', pattern: '%s', good: %b", inputTopic, pattern, !(inputTopic.isEmpty() && pattern.isEmpty())));

        Preconditions.checkArgument(!(inputTopic.isEmpty() && pattern.isEmpty()),
            String.format("Must specify at least one of %s or %s", ConfigConstants.PARAMS_TOPIC_INPUT, ConfigConstants.PARAMS_TOPIC_PATTERN));

        Properties kafkaProperties = readKafkaProperties(params, groupId, true);

        kafkaProperties.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        KafkaSourceBuilder<T> kafkaSourceBuilder = KafkaSource.<T>builder().setDeserializer(deserializationSchema).setProperties(kafkaProperties);

        if (pattern != null) {
            kafkaSourceBuilder.setTopicPattern(Pattern.compile(pattern));
        } else {
            kafkaSourceBuilder.setTopics(inputTopic);
        }

        return env.fromSource(kafkaSourceBuilder.build(), WatermarkStrategy.noWatermarks(), "Kafka Source");
    }

    public static DataStream<MessageToParse> createRawKafkaSource(StreamExecutionEnvironment env, ParameterTool params, String groupId) {
        return createRawKafkaSource(env, params,groupId, new MessageToParseDeserializer());
    }
}
