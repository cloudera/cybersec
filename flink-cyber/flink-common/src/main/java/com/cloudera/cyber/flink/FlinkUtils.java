package com.cloudera.cyber.flink;

import com.cloudera.cyber.IdentifiedMessage;
import com.cloudera.cyber.Message;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.formats.avro.registry.cloudera.ClouderaRegistryKafkaDeserializationSchema;
import org.apache.flink.formats.avro.registry.cloudera.ClouderaRegistryKafkaSerializationSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;

import java.util.Properties;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_ALLOWED_LATENESS;
import static com.cloudera.cyber.flink.ConfigConstants.PARAM_REGISTRY_ADDRESS;
import static com.cloudera.cyber.flink.Utils.*;
import static org.apache.flink.streaming.api.windowing.time.Time.*;

public class FlinkUtils<T extends IdentifiedMessage> {
    private static final long DEFAULT_MAX_LATENESS = 1000;

    public static void setupEnv(StreamExecutionEnvironment env, ParameterTool params) {

    }

    public FlinkKafkaProducer<T> createKafkaSink(final String topic, final ParameterTool params) {
        Properties kafkaProperties = readKafkaProperties(params, false);
        KafkaSerializationSchema<T> schema = ClouderaRegistryKafkaSerializationSchema
                .<T>builder(topic)
                .setRegistryAddress(params.getRequired(PARAM_REGISTRY_ADDRESS))
                .setKey(m -> m.getId())
                .build();
        return new FlinkKafkaProducer<T>(topic,
                schema,
                kafkaProperties,
                FlinkKafkaProducer.Semantic.EXACTLY_ONCE);
    }

    public static FlinkKafkaConsumer<Message> createKafkaSource(String topic, ParameterTool params, String groupId) {
        if (groupId == null || groupId == "") {
            throw new IllegalArgumentException("Must specific group id");
        }
        Properties kafkaProperties = readKafkaProperties(params, true);
        KafkaDeserializationSchema<Message> schema = ClouderaRegistryKafkaDeserializationSchema
                .builder(Message.class)
                .setRegistryAddress(params.getRequired(PARAM_REGISTRY_ADDRESS))
                .build();
        kafkaProperties.put("group.id", groupId);
        FlinkKafkaConsumer<Message> source = new FlinkKafkaConsumer<>(topic, schema, kafkaProperties);

        Time lateness = milliseconds(params.getLong(PARAMS_ALLOWED_LATENESS, DEFAULT_MAX_LATENESS));

        source.assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<Message>(lateness) {
            @Override
            public long extractTimestamp(Message o) {
                return o.getTs();
            }
        });

        return source;
    }
}
