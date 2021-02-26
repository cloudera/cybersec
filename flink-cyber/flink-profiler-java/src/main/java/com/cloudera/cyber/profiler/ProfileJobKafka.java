package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.ConfigConstants;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.addons.hbase.HBaseSinkFunction;
import org.apache.flink.addons.hbase.HBaseWriteOptions;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.regex.Pattern;

import static com.cloudera.cyber.flink.FlinkUtils.createKafkaSource;

public class ProfileJobKafka extends ProfileJob {
    private static final String PROFILE_GROUP_ID = "profile";
    private static final String PARAMS_FIRST_SEEN_HBASE_TABLE = "profile.first.seen.table";
    private static final String PARAMS_FIRST_SEEN_HBASE_COLUMN_FAMILY = "profile.first.seen.column.family";

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

    @Override
    protected DataStream<Message> updateFirstSeen(ParameterTool params, DataStream<Message> results, ProfileGroupConfig profileGroupConfig) {
        String tableName = params.getRequired(PARAMS_FIRST_SEEN_HBASE_TABLE);
        String columnFamilyName = params.getRequired(PARAMS_FIRST_SEEN_HBASE_COLUMN_FAMILY);
        // look up the previous first seen timestamp and update the profile message
        DataStream<Message> updatedProfileMessages = results.map(new FirstSeenHbaseLookup(tableName, columnFamilyName, profileGroupConfig));

        // write the new first and last seen timestamps in hbase
        HBaseSinkFunction<Message> hbaseSink = new FirstSeenHbaseSink(tableName, columnFamilyName, profileGroupConfig);
        hbaseSink.setWriteOptions(HBaseWriteOptions.builder()
                .setBufferFlushIntervalMillis(1000)
                .build()
        );
        updatedProfileMessages.addSink(hbaseSink).name("HBase First Seen Profile Sink");
        return updatedProfileMessages;
    }

}
