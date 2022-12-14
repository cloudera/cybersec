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
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.formats.avro.registry.cloudera.ClouderaRegistryKafkaDeserializationSchema;
import org.apache.flink.formats.avro.registry.cloudera.ClouderaRegistryKafkaSerializationSchema;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Properties;
import java.util.regex.Pattern;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_ALLOWED_LATENESS;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;
import static com.cloudera.cyber.flink.Utils.readSchemaRegistryProperties;
import static org.apache.flink.streaming.api.windowing.time.Time.milliseconds;

@Slf4j
public class FlinkUtils<T> {
    public static final long DEFAULT_MAX_LATENESS = 1000;

    private static final String PARAMS_TOPIC_INPUT = "topic.input";
    private static final String PARAMS_TOPIC_OUTPUT = "topic.output";
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

    public FlinkKafkaProducer<T> createKafkaSink(final String topic, String groupId, final ParameterTool params) {
        Preconditions.checkNotNull(topic, "Must specific output topic");

        Properties kafkaProperties = readKafkaProperties(params, groupId, false);
        log.info("Creating Kafka Sink for {}, using {}", topic, kafkaProperties);
        KafkaSerializationSchema<T> schema = ClouderaRegistryKafkaSerializationSchema
                .<T>builder(topic)
                .setConfig(readSchemaRegistryProperties(params))
                .build();
        return new FlinkKafkaProducer<T>(topic,
                schema,
                kafkaProperties,
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE);
    }

    public FlinkKafkaConsumer<T> createKafkaGenericSource(String topic, ParameterTool params, String groupId) {
        Preconditions.checkNotNull(topic, "Must specific input topic");
        Preconditions.checkNotNull(groupId, "Must specific group id");

        Properties kafkaProperties = readKafkaProperties(params, groupId, true);
        log.info(String.format("Creating Kafka Source for %s, using %s", topic, kafkaProperties));
        KafkaDeserializationSchema<T> schema = ClouderaRegistryKafkaDeserializationSchema
                .builder(type)
                .setConfig(readSchemaRegistryProperties(params))
                .build();

        return new FlinkKafkaConsumer<T>(topic, schema, kafkaProperties);
    }

    public static FlinkKafkaConsumer<Message> createKafkaSource(String topic, ParameterTool params, String groupId) {
        Preconditions.checkNotNull(topic, "Must specific input topic");
        Preconditions.checkNotNull(groupId, "Must specific group id");

        Properties kafkaProperties = readKafkaProperties(params, groupId, true);
        log.info(String.format("Creating Kafka Source for %s, using %s", topic, kafkaProperties));
        KafkaDeserializationSchema<Message> schema = ClouderaRegistryKafkaDeserializationSchema
                .builder(Message.class)
                .setConfig(readSchemaRegistryProperties(params))
                .build();

        FlinkKafkaConsumer<Message> source = new FlinkKafkaConsumer<>(topic, schema, kafkaProperties);

        Time lateness = milliseconds(params.getLong(PARAMS_ALLOWED_LATENESS, DEFAULT_MAX_LATENESS));
        source.assignTimestampsAndWatermarks(new MessageBoundedOutOfOrder(lateness));
        return source;
    }

    public static FlinkKafkaConsumer<Message> createKafkaSource(Pattern topic, ParameterTool params, String groupId) {
        Preconditions.checkNotNull(topic, "Must specific input topic pattern");
        Preconditions.checkNotNull(groupId, "Must specific group id");
        Properties kafkaProperties = readKafkaProperties(params, groupId, true);
        KafkaDeserializationSchema<Message> schema = ClouderaRegistryKafkaDeserializationSchema
                .builder(Message.class)
                .setConfig(readSchemaRegistryProperties(params))
                .build();

        FlinkKafkaConsumer<Message> source = new FlinkKafkaConsumer<>(topic, schema, kafkaProperties);
        Time lateness = milliseconds(params.getLong(PARAMS_ALLOWED_LATENESS, DEFAULT_MAX_LATENESS));
        source.assignTimestampsAndWatermarks(new MessageBoundedOutOfOrder(lateness));
        return source;
    }

    public static <T> DataStream<T> createRawKafkaSource(StreamExecutionEnvironment env, ParameterTool params, String groupId, KafkaDeserializationSchema<T> deserializationSchema) {
        String inputTopic = params.get(ConfigConstants.PARAMS_TOPIC_INPUT,"");
        String pattern = params.get(ConfigConstants.PARAMS_TOPIC_PATTERN, "");

        log.info(String.format("createRawKafkaSource topic: '%s', pattern: '%s', good: %b", inputTopic, pattern, !(inputTopic.isEmpty() && pattern.isEmpty())));

        Preconditions.checkArgument(!(inputTopic.isEmpty() && pattern.isEmpty()),
            String.format("Must specify at least one of %s or %s", ConfigConstants.PARAMS_TOPIC_INPUT, ConfigConstants.PARAMS_TOPIC_PATTERN));

        Properties kafkaProperties = readKafkaProperties(params, groupId, true);

        kafkaProperties.putIfAbsent(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        DataStreamSource<T> source = (pattern != null) ?
            env.addSource(new FlinkKafkaConsumer<>(Pattern.compile(pattern), deserializationSchema, kafkaProperties)) :
            env.addSource(new FlinkKafkaConsumer<>(inputTopic, deserializationSchema, kafkaProperties));

        return source
            .name("Kafka Source")
            .uid("kafka.input");
    }

    public static DataStream<MessageToParse> createRawKafkaSource(StreamExecutionEnvironment env, ParameterTool params, String groupId) {
        return createRawKafkaSource(env, params,groupId, new MessageToParseDeserializer());
    }

    public static DataStream<Message> assignTimestamps(DataStream<Message> messages, long allowedLatenessMillis) {
        BoundedOutOfOrdernessTimestampExtractor<Message> timestampAssigner = new BoundedOutOfOrdernessTimestampExtractor<Message>(Time.milliseconds(allowedLatenessMillis)) {
            @Override
            public long extractTimestamp(Message message) {
                return message.getTs();
            }
        };
        return messages.assignTimestampsAndWatermarks(timestampAssigner);
    }
}
