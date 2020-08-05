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
import static com.cloudera.cyber.flink.ConfigConstants.PARAM_REGISTRY_ADDRESS;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;
import static com.cloudera.cyber.flink.Utils.readSchemaRegistryProperties;
import static org.apache.flink.streaming.api.windowing.time.Time.milliseconds;

@Slf4j
public class FlinkUtils<T> {
    public static final long DEFAULT_MAX_LATENESS = 1000;

    public static final String PARAMS_TOPIC_INPUT = "topic.input";
    public static final String PARAMS_TOPIC_PATTERN = "topic.pattern";
    private static final String PARAMS_CHECKPOINT_INTERVAL = "checkpoint.interval.ms";
    private static final int DEFAULT_CHECKPOINT_INTERVAL = 10000;
    private static final String PARAMS_PARALLELISM = "parallelism";
    private static final int DEFAULT_PARALLELISM = 2;

    public static void setupEnv(StreamExecutionEnvironment env, ParameterTool params) {
        env.enableCheckpointing(params.getInt(PARAMS_CHECKPOINT_INTERVAL, DEFAULT_CHECKPOINT_INTERVAL), CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(params.getInt(PARAMS_PARALLELISM, DEFAULT_PARALLELISM));
    }

    public FlinkKafkaProducer<T> createKafkaSink(final String topic, final ParameterTool params) {
        Preconditions.checkNotNull(topic, "Must specific output topic");

        Properties kafkaProperties = readKafkaProperties(params, false);
        KafkaSerializationSchema<T> schema = ClouderaRegistryKafkaSerializationSchema
                .<T>builder(topic)
                .setRegistryAddress(params.getRequired(PARAM_REGISTRY_ADDRESS))
                .build();
        return new FlinkKafkaProducer<T>(topic,
                schema,
                kafkaProperties,
                FlinkKafkaProducer.Semantic.EXACTLY_ONCE);
    }

    public static FlinkKafkaConsumer<Message> createKafkaSource(String topic, ParameterTool params, String groupId) {
        Preconditions.checkNotNull(topic, "Must specific input topic");
        Preconditions.checkNotNull(groupId, "Must specific group id");

        Properties kafkaProperties = readKafkaProperties(params, true);
        KafkaDeserializationSchema<Message> schema = ClouderaRegistryKafkaDeserializationSchema
                .builder(Message.class)
                .setConfig(readSchemaRegistryProperties(params))
                .build();
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        FlinkKafkaConsumer<Message> source = new FlinkKafkaConsumer<>(topic, schema, kafkaProperties);

        Time lateness = milliseconds(params.getLong(PARAMS_ALLOWED_LATENESS, DEFAULT_MAX_LATENESS));
        source.assignTimestampsAndWatermarks(new MessageBoundedOutOfOrder(lateness));
        return source;
    }

    public static FlinkKafkaConsumer<Message> createKafkaSource(Pattern topic, ParameterTool params, String groupId) {
        Preconditions.checkNotNull(topic, "Must specific input topic pattern");
        Preconditions.checkNotNull(groupId, "Must specific group id");
        Properties kafkaProperties = readKafkaProperties(params, true);
        KafkaDeserializationSchema<Message> schema = ClouderaRegistryKafkaDeserializationSchema
                .builder(Message.class)
                .setConfig(readSchemaRegistryProperties(params))
                .build();
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        FlinkKafkaConsumer<Message> source = new FlinkKafkaConsumer<>(topic, schema, kafkaProperties);

        Time lateness = milliseconds(params.getLong(PARAMS_ALLOWED_LATENESS, DEFAULT_MAX_LATENESS));
        source.assignTimestampsAndWatermarks(new MessageBoundedOutOfOrder(lateness));
        return source;
    }

    public static DataStream<MessageToParse> createRawKafkaSource(StreamExecutionEnvironment env, ParameterTool params, String groupId) {
        Preconditions.checkNotNull(groupId, "Must specific group id");

        String inputTopic = params.get(PARAMS_TOPIC_INPUT,"");
        String pattern = params.get(PARAMS_TOPIC_PATTERN, "");
        log.info(String.format("createRawKafkaSource topic: '%s', pattern: '%s', good: %b", inputTopic, pattern, !(inputTopic.isEmpty() && pattern.isEmpty())));

        Preconditions.checkArgument(!(inputTopic.isEmpty() && pattern.isEmpty()),
                String.format("Must specify at least one of %s or %s", PARAMS_TOPIC_INPUT, PARAMS_TOPIC_PATTERN));

        Properties kafkaProperties = readKafkaProperties(params, true);
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        DataStreamSource<MessageToParse> source = (pattern != null) ?
                env.addSource(new FlinkKafkaConsumer<MessageToParse>(Pattern.compile(pattern), new MessageToParseDeserializer(), kafkaProperties)) :
                env.addSource(new FlinkKafkaConsumer<MessageToParse>(inputTopic, new MessageToParseDeserializer(), kafkaProperties));

        return source
                .name("Kafka Source")
                .uid("kafka.input");
    }
}
