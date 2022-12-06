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

package com.cloudera.cyber.indexing.elastic;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.indexing.CollectionField;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.io.IOException;
import java.time.Instant;
import java.util.Properties;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;

@Slf4j
public class ElasticJobKafka extends ElasticJob {

    private static final String DEFAULT_TOPIC_CONFIG_LOG = "es.config.log";
    public static final String INDEXER_ELASTIC_GROUP_ID = "indexer-elastic";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        new ElasticJobKafka().createPipeline(params).execute("Indexing - Elastic");
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, INDEXER_ELASTIC_GROUP_ID)
        ).name("Kafka Source").uid("kafka-source");
    }

    @Override
    protected DataStream<CollectionField> createConfigSource(StreamExecutionEnvironment env, ParameterTool params) {
        DataStreamSource<CollectionField> dataStreamSource = env.addSource(
                new ElasticTemplateFieldsSource(client,
                        params.getLong(PARAMS_SCHEMA_REFRESH_INTERVAL, DEFAULT_SCHEMA_REFRESH_INTERVAL))
        );
        return dataStreamSource
                .name("Schema Stream").uid("schema-stream")
                .setMaxParallelism(1)
                .setParallelism(1);
    }

    @Override
    protected void logConfig(DataStream<CollectionField> configSource, ParameterTool params) {
        String topic = params.get(PARAMS_TOPIC_CONFIG_LOG, DEFAULT_TOPIC_CONFIG_LOG);
        Properties kafkaProperties = readKafkaProperties(params, INDEXER_ELASTIC_GROUP_ID,false);
        log.info("Creating Kafka Sink for {}, using {}", topic, kafkaProperties);
        FlinkKafkaProducer<CollectionField> kafkaSink = new FlinkKafkaProducer<>(topic,
                (KafkaSerializationSchema<CollectionField>) (collectionField, aLong) -> {
                    ObjectMapper om = new ObjectMapper();
                    try {
                        return new ProducerRecord<>(topic, null, Instant.now().toEpochMilli(), collectionField.getKey().getBytes(), om.writeValueAsBytes(collectionField));
                    } catch (IOException e) {
                        return new ProducerRecord<>(topic, null, Instant.now().toEpochMilli(), collectionField.getKey().getBytes(), e.getMessage().getBytes());
                    }
                },
                kafkaProperties,
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE);

        configSource.addSink(kafkaSink).name("Schema Log").uid("schema-log")
                .setParallelism(1);
    }

}
