package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.parser.MessageToParse;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

import static com.cloudera.cyber.flink.FlinkUtils.PARAMS_TOPIC_PATTERN;
import static com.cloudera.cyber.flink.FlinkUtils.createRawKafkaSource;

@Slf4j
public class SplitJobKafka extends SplitJob {

    private static final String PARAMS_TOPIC_OUTPUT = "topic.output";
    private static final String PARAMS_CONFIG_FILE = "config.file";
    private static final String DEFAULT_CONFIG_FILE = "splits.json";

    public SplitJobKafka(String configJson) {
        this.configJson = configJson;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new RuntimeException("Path to the properties file is expected as the only argument.");
        }

        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
        // need to load the config file locally and put in a property
        String configJson = new String(Files.readAllBytes(Paths.get(params.get(PARAMS_CONFIG_FILE, DEFAULT_CONFIG_FILE))), StandardCharsets.UTF_8);

        log.info(String.format("Splits configuration: %s", configJson));

        StreamExecutionEnvironment env = new SplitJobKafka(configJson)
                .createPipeline(params);
        FlinkUtils.setupEnv(env, params);

        env.execute("Caracal Split Parser");
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<Message>().createKafkaSink(
                params.getRequired(PARAMS_TOPIC_OUTPUT),
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results");
    }

    @Override
    protected DataStream<MessageToParse> createSource(StreamExecutionEnvironment env, ParameterTool params, Iterable<String> topics) {
        log.info(params.mergeWith(ParameterTool.fromMap(Collections.singletonMap(PARAMS_TOPIC_PATTERN, String.join("|", topics)))).toMap().toString());
        return createRawKafkaSource(env,
                params.mergeWith(ParameterTool.fromMap(Collections.singletonMap(PARAMS_TOPIC_PATTERN, String.join("|", topics)))),
                "caracal-splitter");
    }

}
