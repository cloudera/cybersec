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

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.formats.avro.registry.cloudera.ClouderaRegistryKafkaDeserializationSchema;
import org.apache.flink.formats.avro.registry.cloudera.ClouderaRegistryKafkaSerializationSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.cloudera.cyber.flink.Utils.*;
import static java.util.stream.Collectors.toList;

/**
 * TODO - all the request and response stuff should be in wrapper objects instead of using the HasHeader interface.
 * 
 * @param <T>
 */
@Slf4j
public class SourcesWithHeaders<T extends HasHeaders> {
    private final Class<T> type;

    public SourcesWithHeaders(Class<T> type) {
        this.type = type;
    }

    public FlinkKafkaConsumer<T> createSourceWithHeaders(String topic, ParameterTool params, String groupId) {
        Preconditions.checkNotNull(topic, "Must specific input topic");
        Preconditions.checkNotNull(groupId, "Must specific group id");

        Properties kafkaProperties = readKafkaProperties(params, groupId, true);
        log.info(String.format("Creating Kafka Source for %s, using %s", topic, kafkaProperties));
        ClouderaRegistryKafkaDeserializationSchema<Void, T, T> delegate = ClouderaRegistryKafkaDeserializationSchema
                .builder(type)
                .setConfig(readSchemaRegistryProperties(params))
                .build();

        KafkaDeserializationSchema<T> schema = new HeaderDeserializer(delegate);

        return new FlinkKafkaConsumer<T>(topic, schema, kafkaProperties);
    }

    public FlinkKafkaProducer<T> createKafkaSink(final String topic, String groupId, final ParameterTool params) {
        Preconditions.checkNotNull(topic, "Must specific output topic");

        Properties kafkaProperties = readKafkaProperties(params, groupId, false);
        log.info("Creating Kafka Sink for {}, using {}", topic, kafkaProperties);
        ClouderaRegistryKafkaSerializationSchema<Void, T, T> delegate = ClouderaRegistryKafkaSerializationSchema
                .<T>builder(topic)
                .setRegistryAddress(params.getRequired(K_SCHEMA_REG_URL))
                .build();
        HeaderSerializer schema = new HeaderSerializer(delegate);
        return new FlinkKafkaProducer<T>(topic,
                schema,
                kafkaProperties,
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE);
    }
    
    private class HeaderDeserializer implements KafkaDeserializationSchema<T> {
        private ClouderaRegistryKafkaDeserializationSchema<Void, T, T> delegate;

        public HeaderDeserializer(ClouderaRegistryKafkaDeserializationSchema<Void, T, T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean isEndOfStream(T t) {
            return false;
        }

        @Override
        public T deserialize(ConsumerRecord<byte[], byte[]> consumerRecord) throws Exception {
            T deserialize = delegate.deserialize(consumerRecord);
            deserialize.setHeaders(StreamSupport.stream(consumerRecord.headers().spliterator(), false).collect(Collectors.toMap(k -> k.key(), v->new String(v.value()))));
            return deserialize;
        }

        @Override
        public TypeInformation<T> getProducedType() {
            return null;
        }
    }

    private class HeaderSerializer implements KafkaSerializationSchema<T> {
        private final ClouderaRegistryKafkaSerializationSchema<Void, T, T> delegate;

        public HeaderSerializer(ClouderaRegistryKafkaSerializationSchema<Void, T, T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public ProducerRecord<byte[], byte[]> serialize(T t, Long ts) {
            ProducerRecord<byte[], byte[]> serialize = delegate.serialize(t, ts);
            List<Header> headers = t.getHeaders().entrySet().stream().map(h -> new RecordHeader(h.getKey(), h.getValue().getBytes())).collect(toList());
            return new ProducerRecord<>(serialize.topic(), serialize.partition(), serialize.key(), serialize.value(), headers);
        }
    }
}
