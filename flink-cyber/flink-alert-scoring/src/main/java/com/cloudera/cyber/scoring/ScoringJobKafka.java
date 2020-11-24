package com.cloudera.cyber.scoring;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.MessageBoundedOutOfOrder;
import com.cloudera.cyber.rules.DynamicRuleCommandResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.formats.avro.registry.cloudera.ClouderaRegistryKafkaDeserializationSchema;
import org.apache.flink.formats.avro.registry.cloudera.ClouderaRegistryKafkaSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;

import java.util.Properties;
import java.util.regex.Pattern;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_ALLOWED_LATENESS;
import static com.cloudera.cyber.flink.FlinkUtils.createKafkaSource;
import static com.cloudera.cyber.flink.Utils.K_SCHEMA_REG_URL;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;
import static org.apache.flink.streaming.api.windowing.time.Time.milliseconds;

public class ScoringJobKafka extends ScoringJob {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new RuntimeException("Path to the properties file is expected as the only argument.");
        }
        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
        new ScoringJobKafka()
                .createPipeline(params)
                .execute("Flink Scoring");
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<ScoredMessage> results) {
        FlinkKafkaProducer<ScoredMessage> sink = new FlinkUtils<>(ScoredMessage.class).createKafkaSink(
                params.getRequired("topic.output"),
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results");

    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        Pattern inputTopic = Pattern.compile(params.getRequired("topic.pattern"));
        String groupId = "scoring";

        Time lateness = milliseconds(params.getLong(PARAMS_ALLOWED_LATENESS, FlinkUtils.DEFAULT_MAX_LATENESS));
        return env.addSource(createKafkaSource(inputTopic,
                params,
                groupId))
                .name("Kafka Source")
                .uid("kafka.input");
    }

    @Override
    protected DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired("query.input.topic");
        String groupId = "scoring-rules";

        Properties kafkaProperties = readKafkaProperties(params, true);

        KafkaDeserializationSchema<ScoringRuleCommand> schema = ClouderaRegistryKafkaDeserializationSchema
                .builder(ScoringRuleCommand.class)
                .setRegistryAddress(params.getRequired(K_SCHEMA_REG_URL))
                .build();
        kafkaProperties.put("group.id", groupId);
        FlinkKafkaConsumer<ScoringRuleCommand> source = new FlinkKafkaConsumer<>(topic, schema, kafkaProperties);

        return env.addSource(source)
                .name("Kafka Rule Source")
                .uid("kafka.input.rules");
    }

    @Override
    protected void writeQueryResult(ParameterTool params, DataStream<DynamicRuleCommandResult<ScoringRule>> results) {
        String topic = params.getRequired("query.output.topic");
        Properties kafkaProperties = readKafkaProperties(params, false);
        KafkaSerializationSchema<DynamicRuleCommandResult<ScoringRule>> schema = ClouderaRegistryKafkaSerializationSchema
                .<DynamicRuleCommandResult<ScoringRule>>builder(topic)
                .setRegistryAddress(params.getRequired(K_SCHEMA_REG_URL))
                .build();
        FlinkKafkaProducer<DynamicRuleCommandResult<ScoringRule>> sink = new FlinkKafkaProducer<>(topic,
                schema,
                kafkaProperties,
                FlinkKafkaProducer.Semantic.EXACTLY_ONCE);

        results.addSink(sink).name("Kafka Query Results").uid("kafka.results.query");
    }
}
