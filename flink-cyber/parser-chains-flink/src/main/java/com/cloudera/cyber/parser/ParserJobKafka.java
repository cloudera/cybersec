package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.HbaseJobRawKafka;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.flink.ConfigConstants;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
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
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.util.Preconditions;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        Preconditions.checkArgument(args.length >= 1, "Path to the properties files are expected as the only arguments.");
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        FlinkUtils.executeEnv(new ParserJobKafka()
                .createPipeline(params), "Flink Parser", params);

    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired(ConfigConstants.PARAMS_TOPIC_OUTPUT), "cyber-parser",
                params);
        results.addSink(sink).name("Kafka Parser Results ")
                .uid("kafka.results." + results.getTransformation().getUid());
    }

    @Override
    protected void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results) {
        if (!params.getBoolean(PARAMS_ORIGINAL_ENABLED, true)) {
            return;
        }

        // hbase table

        // write the original sources to HDFS files
        Path path = new Path(params.getRequired(PARAMS_ORIGINAL_LOGS_PATH));


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
    protected void writeEnrichments(ParameterTool params, DataStream<EnrichmentCommand> streamingEnrichmentResults, List<String> streamingEnrichmentSources, EnrichmentsConfig streamingEnrichmentConfig) {
        List<String> tables = streamingEnrichmentSources.stream().map(streamingEnrichmentConfig::getReferencedTablesForSource).flatMap(Collection::stream).
                distinct().collect(Collectors.toList());
        HbaseJobRawKafka.enrichmentCommandsToHbase(params, streamingEnrichmentResults, streamingEnrichmentConfig, tables);
    }

    @Override
    protected void writeErrors(ParameterTool params, DataStream<Message> errors) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired(ConfigConstants.PARAMS_TOPIC_ERROR), "cyber-parser",
                params);
        errors.addSink(sink).name("Kafka Error Results " + atomicInteger.get())
                .uid("kafka.error.results." + atomicInteger.getAndIncrement());
    }

    @Override
    protected DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params,
            TopicPatternToChainMap topicPatternToChainMap) {
        return createDataStreamFromMultipleKafkaBrokers(env, params, createGroupId(
                params.get(ConfigConstants.PARAMS_TOPIC_INPUT, "") + params
                        .get(ConfigConstants.PARAMS_TOPIC_PATTERN, "")), topicPatternToChainMap);
    }

    public DataStream<MessageToParse> createDataStreamFromMultipleKafkaBrokers(StreamExecutionEnvironment env,
            ParameterTool params, String groupId, TopicPatternToChainMap topicPatternToChainMap) {
        DataStream<MessageToParse> firstSource = null;
        Map<String, Pattern> brokerTopicPatternMap = topicPatternToChainMap.getBrokerPrefixTopicPatternMap();
        for(Map.Entry<String, Pattern> kafkaPrefixToTopicPattern : brokerTopicPatternMap.entrySet()) {
            String kafkaPrefixConf = kafkaPrefixToTopicPattern.getKey();
            Pattern topicNamePattern = kafkaPrefixToTopicPattern.getValue();
            log.info(String.format("createRawKafkaSource  pattern: '%s', good: %b", topicNamePattern,
                    StringUtils.isNotEmpty(kafkaPrefixConf)));
            Preconditions.checkArgument(StringUtils.isNotEmpty(topicNamePattern.toString()),
                    "Topic name must be specified in chain.topic.map property variable");
            Properties kafkaProperties = Utils.readKafkaProperties(params, groupId, true);
            if (!StringUtils.equals(kafkaPrefixConf, TopicPatternToChainMap.DEFAULT_PREFIX)) {
                Properties brokerSpecificProperties = Utils
                        .readProperties(params.getProperties(), kafkaPrefixConf + '.' + Utils.KAFKA_PREFIX);
                kafkaProperties.putAll(brokerSpecificProperties);
            }
            DataStreamSource<MessageToParse> streamSource = env.addSource(
                    new FlinkKafkaConsumer<>(topicNamePattern, new MessageToParseDeserializer(), kafkaProperties));
            SingleOutputStreamOperator<MessageToParse> newSource = streamSource
                    .name(String.format("Kafka Source topic='%s' kafka prefix configuration='%s'", topicNamePattern.toString(), kafkaPrefixConf))
                    .uid("kafka.input." + kafkaPrefixConf);

            if (firstSource == null) {
                firstSource = newSource;
            } else {
                firstSource.union(newSource);
            }
        }
        if (firstSource == null) {
            Preconditions.checkNotNull("No topics were read by the topic map configuration.");
        }
        return firstSource;
    }


    private String createGroupId(String inputTopic) {
        return "cyber-parser-" + DigestUtils.md5DigestAsHex(inputTopic.getBytes(StandardCharsets.UTF_8));
    }
}
