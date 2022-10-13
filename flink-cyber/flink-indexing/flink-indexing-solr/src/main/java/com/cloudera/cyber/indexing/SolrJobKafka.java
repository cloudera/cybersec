package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.formats.avro.registry.cloudera.ClouderaRegistryKafkaSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.flink.util.Preconditions;

import java.util.Arrays;
import java.util.Properties;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.Utils.K_SCHEMA_REG_URL;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;

@Slf4j
public class SolrJobKafka extends SolrJob {
    private static final String DEFAULT_TOPIC_CONFIG_LOG = "solr.config.log";


    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        new SolrJobKafka().createPipeline(Utils.getParamToolsFromProperties(args)).execute("Indexing - Solr");
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, "indexer-solr")
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
        Properties kafkaProperties = readKafkaProperties(params, "indexer-solr-schema", false);
        log.info("Creating Kafka Sink for {}, using {}", topic, kafkaProperties);
        KafkaSerializationSchema<CollectionField> schema = ClouderaRegistryKafkaSerializationSchema
                .<CollectionField>builder(topic)
                .setRegistryAddress(params.getRequired(K_SCHEMA_REG_URL))
                .build();
        FlinkKafkaProducer<CollectionField> kafkaSink = new FlinkKafkaProducer<>(topic,
                schema,
                kafkaProperties,
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE);
        configSource.addSink(kafkaSink).name("Schema Log").uid("schema-log")
                .setParallelism(1);
    }
}
