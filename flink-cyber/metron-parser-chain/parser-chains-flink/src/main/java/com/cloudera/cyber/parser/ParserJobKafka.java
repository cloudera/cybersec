package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.avro.ParquetAvroWriters;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.OutputFileConfig;
import org.apache.flink.streaming.api.functions.sink.filesystem.StreamingFileSink;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.DefaultRollingPolicy;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static com.cloudera.cyber.flink.FlinkUtils.createRawKafkaSource;

public class ParserJobKafka extends ParserJob {

    public static final String PARAMS_ORIGINAL_LOGS_PATH = "original.basepath";
    public static final String PARAMS_ROLL_PART_SIZE = "original.roll.size";
    public static final String PARAMS_ROLL_INACTIVITY = "original.roll.inactive" ;
    public static final String PARAMS_ROLL_INTERVAL = "original.roll.interval";
    public static final long DEFAULT_ROLL_INTERVAL = TimeUnit.MINUTES.toMicros(15);
    public static final long DEFAULT_ROLL_PART_SIZE = 1024*1024*128;
    public static final long DEFAULT_ROLL_INACTIVITY = TimeUnit.MINUTES.toMicros(5);
    public static final String PARAMS_ORIGINAL_ENABLED = "original.enabled";
    public static final String PARAMS_CONFIG_TOPIC = "config.topic";
    ;

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
        FlinkKafkaProducer<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired("topic.output"),
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

        StreamingFileSink<MessageToParse> sink = StreamingFileSink
                .forBulkFormat(path, ParquetAvroWriters.forSpecificRecord(MessageToParse.class))
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
    protected DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return createRawKafkaSource(env, params, createGroupId(params.get("topic.input", "") + params.get("topic.pattern", "")));
    }

    private String createGroupId(String inputTopic) {
        return "cyber-parser-" + DigestUtils.md5DigestAsHex(inputTopic.getBytes(StandardCharsets.UTF_8));
    }
}
