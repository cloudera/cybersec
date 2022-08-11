package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.parser.MessageToParse;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.streaming.connectors.kafka.KafkaSerializationSchema;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

import static com.cloudera.cyber.flink.ConfigConstants.*;
import static com.cloudera.cyber.flink.FlinkUtils.createRawKafkaSource;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;
import static com.cloudera.cyber.parser.ParserJobKafka.*;

@Slf4j
public class SplitJobKafka extends SplitJob {

    private static final String PARAMS_CONFIG_FILE = "config.file";
    private static final String DEFAULT_CONFIG_FILE = "splits.json";
    private static final String PARAM_COUNT_TOPIC = "count.topic";
    private static final String DEFAULT_COUNT_TOPIC = "parser.counts";

    public SplitJobKafka(String configJson) {
        this.configJson = configJson;
    }

    public static void main(String[] args) throws Exception {

        ParameterTool params = Utils.getParamToolsFromProperties(args);
        // need to load the config file locally and put in a property
        String configJson = new String(Files.readAllBytes(Paths.get(params.get(PARAMS_CONFIG_FILE, DEFAULT_CONFIG_FILE))), StandardCharsets.UTF_8);

        log.info(String.format("Splits configuration: %s", configJson));

        StreamExecutionEnvironment env = new SplitJobKafka(configJson)
                .createPipeline(params);
        FlinkUtils.setupEnv(env, params);

        env.execute("Caracal Split Parser");
    }

    @Override
    protected DataStream<SplitConfig> createConfigSource(StreamExecutionEnvironment env, ParameterTool params) {
        String groupId = createGroupId(params.get(PARAMS_TOPIC_INPUT, "") + params.get(PARAMS_TOPIC_PATTERN, ""), "cyber-split-parser-config-");
        Properties kafkaProperties = readKafkaProperties(params, groupId, true);

        FlinkKafkaConsumer<String> source =
                new FlinkKafkaConsumer<>(params.getRequired(PARAMS_CONFIG_TOPIC),  new SimpleStringSchema(), kafkaProperties);

        return env.addSource(source)
                .name("Config Kafka Feed").uid("config.source.kafka").setParallelism(1).setMaxParallelism(1)
                .map(new SplitConfigJsonParserMap())
                .name("Config Source").uid("config.source").setMaxParallelism(1).setParallelism(1);

    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired(PARAMS_TOPIC_OUTPUT),
                "splits-parser",
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results");
    }

    @Override
    protected void writeOriginalsResults(ParameterTool params, DataStream<MessageToParse> results) {
        if (!params.getBoolean(PARAMS_ORIGINAL_ENABLED, true)) return;

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

        results.addSink(sink).name("Original Archiver").uid("original.archiver");
    }

    @Override
    protected void writeCounts(ParameterTool params, DataStream<Tuple2<String, Long>> sums) {
        Properties kafkaProperties = readKafkaProperties(params, "splits-parser", false);
        String topic = params.get(PARAM_COUNT_TOPIC, DEFAULT_COUNT_TOPIC);

        sums.addSink(new FlinkKafkaProducer<>(topic,
                (KafkaSerializationSchema<Tuple2<String, Long>>) (e, time) ->
                        new ProducerRecord<>(topic, null, time, e.f0.getBytes(), e.f1.toString().getBytes()),
                kafkaProperties,
                FlinkKafkaProducer.Semantic.AT_LEAST_ONCE)
        ).name("Count Results").uid("count.results");
    }

    @Override
    protected DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params, Iterable<String> topics) {
        log.info(params.mergeWith(ParameterTool.fromMap(Collections.singletonMap(PARAMS_TOPIC_PATTERN, String.join("|", topics)))).toMap().toString());
        return createRawKafkaSource(env,
                params.mergeWith(ParameterTool.fromMap(Collections.singletonMap(PARAMS_TOPIC_PATTERN, String.join("|", topics)))),
                createGroupId(params.get("topic.input", "") + params.get("topic.pattern", ""), "cyber-split-parser-"));
    }

    private String createGroupId(String inputTopic, String prefix) {
        return prefix + DigestUtils.md5DigestAsHex(inputTopic.getBytes(StandardCharsets.UTF_8));
    }


}
