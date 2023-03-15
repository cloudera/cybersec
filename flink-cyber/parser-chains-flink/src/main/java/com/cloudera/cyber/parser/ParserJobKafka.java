/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

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
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;
import org.apache.flink.util.Preconditions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class ParserJobKafka extends ParserJob {

    private final AtomicInteger atomicInteger = new AtomicInteger(0);

    public static final String PARAMS_ORIGINAL_LOGS_PATH = "original.basepath";
    public static final String PARAMS_ORIGINAL_ENABLED = "original.enabled";
    public static final String PARAMS_CONFIG_TOPIC = "config.topic";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        FlinkUtils.executeEnv(new ParserJobKafka()
                .createPipeline(params), "Flink Parser", params);

    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        KafkaSink<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired(ConfigConstants.PARAMS_TOPIC_OUTPUT), "cyber-parser",
                params);
        results.sinkTo(sink).name("Kafka Parser Results ")
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
        KafkaSink<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired(ConfigConstants.PARAMS_TOPIC_ERROR), "cyber-parser",
                params);
        errors.sinkTo(sink).name("Kafka Error Results " + atomicInteger.get())
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
            KafkaSource<MessageToParse> rawMessages = KafkaSource.<MessageToParse>builder()
                    .setTopicPattern(topicNamePattern).setBootstrapServers(kafkaProperties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)).setDeserializer(new MessageToParseDeserializer()).setProperties(kafkaProperties).build();
            DataStreamSource<MessageToParse> streamSource = env.fromSource(rawMessages, WatermarkStrategy.noWatermarks(), "Kafka Raw Messages");
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
