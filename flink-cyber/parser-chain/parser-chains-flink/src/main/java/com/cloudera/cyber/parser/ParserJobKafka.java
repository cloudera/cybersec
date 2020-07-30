package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.regex.Pattern;

import static com.cloudera.cyber.flink.Utils.readKafkaProperties;

public class ParserJobKafka extends ParserJob {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new RuntimeException("Path to the properties file is expected as the only argument.");
        }
        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
        new ParserJobKafka()
                .createPipeline(params)
                .execute("Flink Parser - " + params.get("name"));
    }
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
