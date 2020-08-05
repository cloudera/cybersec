package com.cloudera.flink;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import static com.cloudera.cyber.flink.FlinkUtils.createKafkaSource;

public class SampleKakfaJob {
    private static final String PARAM_CHECKPOINT_INTERVAL = "checkpoint.interval.ms";

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new RuntimeException("Path to the properties file is expected as the only argument.");
        }

        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);


        new SampleKakfaJob()
                .createPipeline(params)
                .execute("Caracal Split Parser");
    }

    private StreamExecutionEnvironment createPipeline(ParameterTool params) {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        env.enableCheckpointing(params.getInt(PARAM_CHECKPOINT_INTERVAL, 10000), CheckpointingMode.EXACTLY_ONCE);

        DataStream<Message> results = createSource(env, params);
        writeResults(params, results);
        return env;
    }

    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<Message>().createKafkaSink(
                "test.output",
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results");
    }

    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        String inputTopic = "dpi_http";
        String groupId = "test-kafka";
        return env.addSource(createKafkaSource(inputTopic,
                params,
                groupId))
                .name("Kafka Source")
                .uid("kafka.input");
    }
}
