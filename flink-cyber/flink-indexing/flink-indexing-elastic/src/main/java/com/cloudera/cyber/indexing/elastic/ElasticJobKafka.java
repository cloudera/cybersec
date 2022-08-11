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
