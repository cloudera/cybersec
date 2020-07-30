package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.TimedBoundedOutOfOrdernessTimestampExtractor;
import com.cloudera.parserchains.core.Regex;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.formats.avro.registry.cloudera.ClouderaRegistryKafkaDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaDeserializationSchema;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_ALLOWED_LATENESS;
import static com.cloudera.cyber.flink.FlinkUtils.createKafkaSource;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;
import static com.cloudera.cyber.flink.Utils.readSchemaRegistryProperties;
import static org.apache.flink.streaming.api.windowing.time.Time.milliseconds;

public class ParserJobKafka extends ParserJob {
    private static final String PARAM_LATENESS = "lateness";

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<Message>().createKafkaSink(
                params.getRequired("topic.output"),
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results");
    }

    @Override
    protected DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        String inputTopic = params.get("topic.input");
        String pattern = params.get("topic.pattern");
        String groupId = createGroupId(inputTopic);

        Properties kafkaProperties = readKafkaProperties(params, true);
        kafkaProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        DataStreamSource<MessageToParse> source = (pattern != null) ?
                env.addSource(new FlinkKafkaConsumer<MessageToParse>(Pattern.compile(pattern), new MessageToParseDeserializer(), kafkaProperties)) :
                env.addSource(new FlinkKafkaConsumer<MessageToParse>(inputTopic, new MessageToParseDeserializer(), kafkaProperties));

        return source
                .name("Kafka Source")
                .uid("kafka.input");
    }

    private String createGroupId(String inputTopic) {
        return "cyber-parser-" + DigestUtils.md5DigestAsHex(inputTopic.getBytes(StandardCharsets.UTF_8));
    }
}
