package com.cloudera.cyber.sessions;

import com.cloudera.cyber.GroupedMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.Arrays;
import java.util.List;

import static com.cloudera.cyber.flink.FlinkUtils.createKafkaSource;

public class SessionJobKafka extends SessionJob {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new RuntimeException("Path to the properties file is expected as the only argument.");
        }
        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
        StreamExecutionEnvironment env = new SessionJobKafka().createPipeline(params);
        env.execute("Flink Sessionizer");
    }

    /**
     * Returns a consumer group id for the sessioniser ensuring that each topic is only processed once with the same keys
     *
     * @param inputTopic topic to read from
     * @param sessionKey the keys being used to sessionise
     * @param sessionTimeout
     * @return
     */
    private String createGroupId(String inputTopic, List<String> sessionKey, Long sessionTimeout) {
        List<String> parts = Arrays.asList("sessionizer",
                inputTopic,
                String.valueOf(sessionKey.hashCode()),
                String.valueOf(sessionTimeout));
        return String.join(".", parts);
    }

    @Override
    protected void writeResults(ParameterTool params, SingleOutputStreamOperator<GroupedMessage> results) {
        FlinkKafkaProducer<GroupedMessage> sink = new FlinkUtils<GroupedMessage>().createKafkaSink(
                params.getRequired("topic.enrichment"),
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results");
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> sessionKey, Long sessionTimeout) {
        String inputTopic = params.getRequired("topic.input");
        String groupId = createGroupId(inputTopic, sessionKey, sessionTimeout);
        return
                env.addSource(createKafkaSource(inputTopic,
                        params,
                        groupId))
                        .name("Kafka Source")
                        .uid("kafka.input");
    }
}
