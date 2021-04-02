package com.cloudera.cyber.parser;

import static com.cloudera.cyber.parser.Utils.DEFAULT_BROKER;
import static com.cloudera.cyber.parser.Utils.getBrokerTopicPatternMap;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.ConfigConstants;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.util.Preconditions;
import org.springframework.util.DigestUtils;

@Slf4j
public class ParserJobKafka extends ParserJob {

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    public static final String PARAMS_ORIGINAL_LOGS_PATH = "original.basepath";
    public static final String PARAMS_ROLL_PART_SIZE = "original.roll.size";
    public static final String PARAMS_ROLL_INACTIVITY = "original.roll.inactive";
    public static final String PARAMS_ROLL_INTERVAL = "original.roll.interval";
    public static final long DEFAULT_ROLL_INTERVAL = TimeUnit.MINUTES.toMicros(15);
    public static final long DEFAULT_ROLL_PART_SIZE = 1024 * 1024 * 128;
    public static final long DEFAULT_ROLL_INACTIVITY = TimeUnit.MINUTES.toMicros(5);
    public static final String PARAMS_ORIGINAL_ENABLED = "original.enabled";
    public static final String PARAMS_CONFIG_TOPIC = "config.topic";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Path to the properties file is expected as the only argument.");
        }
        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
        new ParserJobKafka()
                .createPipeline(params)
                .execute("Flink Parser - " + params.get("name", "Default"));
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired(ConfigConstants.PARAMS_TOPIC_OUTPUT), "cyber-parser",
                params);
        results.addSink(sink).name("Kafka Results ")
                .uid("kafka.results." + results.getTransformation().getUid());
    }

    @Override
    protected void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results) {
        if (!params.getBoolean(PARAMS_ORIGINAL_ENABLED, true)) {
            return;
        }

        // write the original sources to HDFS files
        Path path = new Path(params.getRequired(PARAMS_ORIGINAL_LOGS_PATH));

        DefaultRollingPolicy<MessageToParse, String> defaultRollingPolicy = DefaultRollingPolicy.builder()
                .withInactivityInterval(params.getLong(PARAMS_ROLL_INACTIVITY, DEFAULT_ROLL_INACTIVITY))
                .withMaxPartSize(params.getLong(PARAMS_ROLL_PART_SIZE, DEFAULT_ROLL_PART_SIZE))
                .withRolloverInterval(params.getLong(PARAMS_ROLL_INTERVAL, DEFAULT_ROLL_INTERVAL))
                .build();

        // TODO - add the message id
        // TODO - add filtering (might not care about all raws)
        // TODO - change the factory to support compression
        StreamingFileSink<MessageToParse> sink = StreamingFileSink
                .forBulkFormat(path, ParquetAvroWriters.forReflectRecord(MessageToParse.class))
                .withRollingPolicy(OnCheckpointRollingPolicy.build())
                .withOutputFileConfig(OutputFileConfig
                        .builder()
                        .withPartPrefix("logs")
                        .withPartSuffix(".parquet")
                        .build())
                .build();

        results.addSink(sink).name("Original Archiver ")
                .uid("original.archiver." + results.getTransformation().getUid());
    }

    @Override
    protected void writeErrors(ParameterTool params, DataStream<Message> errors) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired(ConfigConstants.PARAMS_TOPIC_ERROR), "cyber-parser",
                params);
        errors.addSink(sink).name("Kafka Results " + atomicInteger.get())
                .uid("kafka.error.results." + atomicInteger.getAndIncrement());
    }

    @Override
    protected List<DataStream<MessageToParse>> createSource(StreamExecutionEnvironment env, ParameterTool params,
            TopicPatternToChainMap topicPatternToChainMap) {
        return createDataStreamFromMultipleKafkaBrokers(env, params, createGroupId(
                params.get(ConfigConstants.PARAMS_TOPIC_INPUT, "") + params
                        .get(ConfigConstants.PARAMS_TOPIC_PATTERN, "")), topicPatternToChainMap);
    }

    public List<DataStream<MessageToParse>> createDataStreamFromMultipleKafkaBrokers(StreamExecutionEnvironment env,
            ParameterTool params, String groupId, TopicPatternToChainMap topicPatternToChainMap) {
        List<DataStream<MessageToParse>> sources = new ArrayList<>();
        Map<String, Pattern> brokerTopicPatternMap = getBrokerTopicPatternMap(topicPatternToChainMap);
        brokerTopicPatternMap.forEach((brokerName, topicNamePattern) -> {
            log.info(String.format("createRawKafkaSource  pattern: '%s', good: %b", topicNamePattern,
                    StringUtils.isNotEmpty(brokerName)));
            Preconditions.checkArgument(StringUtils.isNotEmpty(topicNamePattern.toString()),
                    "Topic name must be specified in chain.topic.map property variable");
            Properties kafkaProperties = Utils.readKafkaProperties(params, groupId, true);
            if (!StringUtils.equals(brokerName, DEFAULT_BROKER)) {
                Properties brokerSpecificProperties = Utils
                        .readProperties(params.getProperties(), brokerName + '.' + Utils.KAFKA_PREFIX);
                kafkaProperties.putAll(brokerSpecificProperties);
            }
            DataStreamSource<MessageToParse> streamSource = env.addSource(
                    new FlinkKafkaConsumer<>(topicNamePattern, new MessageToParseDeserializer(), kafkaProperties));
            SingleOutputStreamOperator<MessageToParse> source = streamSource
                    .name(String.format("Kafka Source topic='%s' broker='%s'", topicNamePattern.toString(), brokerName))
                    .uid("kafka.input." + brokerName);
            sources.add(source);
        });
        return sources;
    }


    private String createGroupId(String inputTopic) {
        return "cyber-parser-" + DigestUtils.md5DigestAsHex(inputTopic.getBytes(StandardCharsets.UTF_8));
    }
}
