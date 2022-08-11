package com.cloudera.cyber.scoring;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;

import java.util.regex.Pattern;

import static com.cloudera.cyber.flink.FlinkUtils.createKafkaSource;

public class ScoringJobKafka extends ScoringJob {

    private static final String SCORING_GROUP_ID = "scoring";
    private static final String SCORING_RULES_GROUP_ID = "scoring-rules";

    public static void main(String[] args) throws Exception {
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        FlinkUtils.executeEnv(new ScoringJobKafka()
                .createPipeline(params), "Flink Scoring", params);
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<ScoredMessage> results) {
        FlinkKafkaProducer<ScoredMessage> sink = new FlinkUtils<>(ScoredMessage.class).createKafkaSink(
                params.getRequired("topic.output"), SCORING_GROUP_ID,
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results");

    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        Pattern inputTopic = Pattern.compile(params.getRequired("topic.pattern"));

        return env.addSource(createKafkaSource(inputTopic,
                params,
                SCORING_GROUP_ID))
                .name("Kafka Source")
                .uid("kafka.input");
    }

    @Override
    protected DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired("query.input.topic");

        FlinkKafkaConsumer<ScoringRuleCommand> source = new FlinkUtils<>(ScoringRuleCommand.class).createKafkaGenericSource(topic, params, SCORING_RULES_GROUP_ID);

        return env.addSource(source)
                .name("Kafka Rule Source")
                .uid("kafka.input.rule.command");
    }

    @Override
    protected void writeQueryResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results) {
        String topic = params.getRequired("query.output.topic");
        FlinkKafkaProducer<ScoringRuleCommandResult> sink = new FlinkUtils<>(ScoringRuleCommandResult.class).createKafkaSink(topic, SCORING_RULES_GROUP_ID, params);
        results.addSink(sink).name("Kafka Rule Command Results").uid("kafka.output.rule.command.results");
    }
}
