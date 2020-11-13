package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.codehaus.jackson.map.ObjectMapper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Properties;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;

@Slf4j
public class SolrJobKafka extends SolrJob {
    private static final String DEFAULT_TOPIC_CONFIG_LOG = "solr.config.log";


    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length == 1, "Arguments must consist of a single properties file");
        new SolrJobKafka().createPipeline(ParameterTool.fromPropertiesFile(args[0])).execute("Indexing - Solr");
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils(Message.class).createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, "indexer-solr")
        ).name("Message Source").uid("message-source");
    }

    protected DataStream<CollectionField> createConfigSource(StreamExecutionEnvironment env, ParameterTool params) {
        DataStreamSource<CollectionField> dataStreamSource = env.addSource(
                new SolrCollectionFieldsSource(Arrays.asList(params.getRequired("solr.urls").split(",")),
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
        Properties kafkaProperties = readKafkaProperties(params, false);
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
