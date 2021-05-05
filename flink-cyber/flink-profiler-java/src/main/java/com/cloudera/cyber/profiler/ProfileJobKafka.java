package com.cloudera.cyber.profiler;

import com.cloudera.cyber.flink.ConfigConstants;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.ScoringRuleCommand;
import com.cloudera.cyber.scoring.ScoringRuleCommandResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.hbase.sink.HBaseSinkFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;

public class ProfileJobKafka extends ProfileJob {
    private static final String PROFILE_GROUP_ID = "profile";
    private static final String PARAMS_FIRST_SEEN_HBASE_TABLE = "profile.first.seen.table";
    private static final String PARAMS_FIRST_SEEN_HBASE_COLUMN_FAMILY = "profile.first.seen.column.family";
    private static final String PARAMS_GROUP_ID = "kafka.group.id";

    private FlinkKafkaProducer<ScoredMessage> sink;

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new RuntimeException("Path to the properties file is expected as the only argument.");
        }
        ParameterTool params = ParameterTool.fromPropertiesFile(args[0]);
        FlinkUtils.executeEnv(new ProfileJobKafka()
                .createPipeline(params), "Flink Profiling",  params);
    }

    @Override
    protected DataStream<ScoredMessage> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils<>(ScoredMessage.class).createKafkaGenericSource(params.getRequired(PARAMS_TOPIC_INPUT), params, params.get(PARAMS_GROUP_ID, PROFILE_GROUP_ID))
        ).name("Kafka Source").uid("kafka-source");
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<ScoredMessage> results) {

        if (sink == null) {
            sink = new FlinkUtils<>(ScoredMessage.class).createKafkaSink(
                    params.getRequired(ConfigConstants.PARAMS_TOPIC_OUTPUT), PROFILE_GROUP_ID,
                    params);
        }
        results.addSink(sink).name("Kafka Results").uid("kafka.results.");
    }

    @Override
    protected DataStream<ProfileMessage> updateFirstSeen(ParameterTool params, DataStream<ProfileMessage> results, ProfileGroupConfig profileGroupConfig) {
        String tableName = params.getRequired(PARAMS_FIRST_SEEN_HBASE_TABLE);
        String columnFamilyName = params.getRequired(PARAMS_FIRST_SEEN_HBASE_COLUMN_FAMILY);
        // look up the previous first seen timestamp and update the profile message
        DataStream<ProfileMessage> updatedProfileMessages = results.map(new FirstSeenHbaseLookup(tableName, columnFamilyName, profileGroupConfig));

        // write the new first and last seen timestamps in hbase
        HBaseSinkFunction<ProfileMessage> hbaseSink = new FirstSeenHbaseSink(tableName, columnFamilyName, profileGroupConfig, params);
        updatedProfileMessages.addSink(hbaseSink).name("HBase First Seen Profile Sink");
        return updatedProfileMessages;
    }

    @Override
    protected DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired("query.input.topic");
        FlinkKafkaConsumer<ScoringRuleCommand> source = new FlinkUtils<>(ScoringRuleCommand.class).createKafkaGenericSource(topic, params, params.get(PARAMS_GROUP_ID, PROFILE_GROUP_ID));
        return env.addSource(source)
                .name("Kafka Score Rule Source")
                .uid("kafka.input.rule.command");
    }

    @Override
    protected void writeScoredRuleCommandResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results) {
        String topic = params.getRequired("query.output.topic");
        FlinkKafkaProducer<ScoringRuleCommandResult> sink = new FlinkUtils<>(ScoringRuleCommandResult.class).createKafkaSink(topic, params.get(PARAMS_GROUP_ID, PROFILE_GROUP_ID), params);
        results.addSink(sink).name("Kafka Score Rule Command Results").uid("kafka.output.rule.command.results");

    }

}
