package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.formats.avro.registry.cloudera.ClouderaRegistryKafkaDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.consumer.ConsumerConfig;

import java.util.Properties;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;
import static com.cloudera.cyber.flink.Utils.readSchemaRegistryProperties;

@Slf4j
public class HiveJobKakfa extends HiveJob {

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length == 1, "Arguments must consist of a single properties file");
        new HiveJobKakfa().createPipeline(ParameterTool.fromPropertiesFile(args[0])).execute("Indexing - Hive");
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired(PARAMS_TOPIC_INPUT);
        String groupId = "indexer-hive";


        Properties kafkaProperties = readKafkaProperties(params, true);
        log.info(String.format("Creating Kafka Source for %s, using %s", topic, kafkaProperties));
        KafkaDeserializationSchema<Message> schema = ClouderaRegistryKafkaDeserializationSchema
                .builder(Message.class)
                .setConfig(readSchemaRegistryProperties(params))
                .build();
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        // for the SMM interceptor
        kafkaProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, groupId);

        FlinkKafkaConsumer<Message> source = new FlinkKafkaConsumer<>(topic, schema, kafkaProperties);
        return env.addSource(source).name("Kafka Source").uid("kafka-source");

 /*       return env.addSource(FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, "indexer-hive"))
                ;*/
    }
}
