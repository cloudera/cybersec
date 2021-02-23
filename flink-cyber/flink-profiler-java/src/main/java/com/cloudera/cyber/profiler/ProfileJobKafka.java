package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.ConfigConstants;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.regex.Pattern;

import static com.cloudera.cyber.flink.FlinkUtils.createKafkaSource;

public class ProfileJobKafka extends ProfileJob {
    private static final String PROFILE_GROUP_ID = "profile";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new RuntimeException("Path to the properties file is expected as the only argument.");
        }
        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
        FlinkUtils.executeEnv(new ProfileJobKafka()
                .createPipeline(params), "Flink Profiling",  params);
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        Pattern inputTopic = Pattern.compile(params.getRequired(ConfigConstants.PARAMS_TOPIC_INPUT));

        return env.addSource(createKafkaSource(inputTopic,
                params,
                PROFILE_GROUP_ID))
                .name("Kafka Source")
                .uid("kafka.input");
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results, String profileGroupName) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired(ConfigConstants.PARAMS_TOPIC_OUTPUT), PROFILE_GROUP_ID,
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results." + profileGroupName);
    }
}
