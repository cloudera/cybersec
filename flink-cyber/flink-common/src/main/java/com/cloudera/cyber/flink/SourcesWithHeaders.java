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

import static com.cloudera.cyber.flink.Utils.readKafkaProperties;
import static com.cloudera.cyber.flink.Utils.readSchemaRegistryProperties;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.formats.registry.cloudera.avro.ClouderaRegistryAvroKafkaDeserializationSchema;
import org.apache.flink.formats.registry.cloudera.avro.ClouderaRegistryAvroKafkaRecordSerializationSchema;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.util.Collector;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;

/**
 * TODO - all the request and response stuff should be in wrapper objects instead of using the HasHeader interface.
 */
@Slf4j
public class SourcesWithHeaders<T extends HasHeaders> {
    private final Class<T> type;

    public SourcesWithHeaders(Class<T> type) {
        this.type = type;
    }

    public KafkaSource<T> createSourceWithHeaders(String topic, ParameterTool params, String groupId) {
        Preconditions.checkNotNull(topic, "Must specific input topic");
        Preconditions.checkNotNull(groupId, "Must specific group id");

        Preconditions.checkNotNull(groupId, "Must specific group id");

        Properties kafkaProperties = readKafkaProperties(params, groupId, true);
        KafkaDeserializationSchema<T> delegate = ClouderaRegistryAvroKafkaDeserializationSchema
              .builder(type)
              .setConfig(readSchemaRegistryProperties(params))
              .build();

        KafkaRecordDeserializationSchema<T> schema = new HeaderDeserializer(delegate);

        return KafkaSource.<T>builder().setTopics(topic).setDeserializer(schema)
              .setProperties(kafkaProperties).build();
    }

    public KafkaSink<T> createKafkaSink(final String topic, String groupId, final ParameterTool params) {
        Preconditions.checkNotNull(topic, "Must specific output topic");

        Properties kafkaProperties = readKafkaProperties(params, groupId, false);
        log.info("Creating Kafka Sink for {}, using {}", topic, kafkaProperties);

        KafkaRecordSerializationSchema<T> delegate = ClouderaRegistryAvroKafkaRecordSerializationSchema
              .<T>builder(topic)
              .setConfig(readSchemaRegistryProperties(params))
              .build();

        HeaderSerializer schema = new HeaderSerializer(delegate);
        return KafkaSink.<T>builder()
              .setKafkaProducerConfig(kafkaProperties)
              .setRecordSerializer(schema)
              .setDeliverGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
              .build();
    }

    private class HeaderDeserializer implements KafkaRecordDeserializationSchema<T> {
        private final KafkaDeserializationSchema<T> delegate;

        public HeaderDeserializer(KafkaDeserializationSchema<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public TypeInformation<T> getProducedType() {
            return TypeInformation.of(type);
        }

        @Override
        public void deserialize(ConsumerRecord<byte[], byte[]> consumerRecord, Collector<T> collector) {
            try {
                T deserialize = delegate.deserialize(consumerRecord);
                deserialize.setHeaders(StreamSupport.stream(consumerRecord.headers().spliterator(), false)
                      .collect(Collectors.toMap(Header::key, v -> new String(v.value()))));
                collector.collect(deserialize);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class HeaderSerializer implements KafkaRecordSerializationSchema<T> {
        private final KafkaRecordSerializationSchema<T> delegate;

        public HeaderSerializer(KafkaRecordSerializationSchema<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public ProducerRecord<byte[], byte[]> serialize(T t, KafkaSinkContext kafkaSinkContext, Long ts) {
            ProducerRecord<byte[], byte[]> serialize = delegate.serialize(t, kafkaSinkContext, ts);
            List<Header> headers =
                  t.getHeaders().entrySet().stream().map(h -> new RecordHeader(h.getKey(), h.getValue().getBytes()))
                        .collect(toList());
            return new ProducerRecord<>(serialize.topic(), serialize.partition(), serialize.key(), serialize.value(),
                  headers);

        }
    }
}
