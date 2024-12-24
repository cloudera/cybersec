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

package com.cloudera.cyber.scoring;

import static com.cloudera.cyber.flink.FlinkUtils.createKafkaSource;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import java.util.regex.Pattern;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

public class ScoringJobKafka extends ScoringJob {

    private static final String SCORING_GROUP_ID = "scoring";
    private static final String SCORING_RULES_GROUP_ID = "scoring-rules";
    private static final String SCORING_SUMMARIZATION = "scoring.summarization";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        FlinkUtils.executeEnv(new ScoringJobKafka()
              .createPipeline(params), "Flink Scoring", params);
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<ScoredMessage> results) {
        KafkaSink<ScoredMessage> sink = new FlinkUtils<>(ScoredMessage.class).createKafkaSink(
              params.getRequired("topic.output"), SCORING_GROUP_ID,
              params);
        results.sinkTo(sink).name("Kafka Results").uid("kafka.results");

    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        Pattern inputTopic = Pattern.compile(params.getRequired("topic.pattern"));

        return env.fromSource(createKafkaSource(inputTopic,
                    params,
                    SCORING_GROUP_ID), WatermarkStrategy.noWatermarks(), "Kafka Source")
              .uid("kafka.input");
    }

    @Override
    protected DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired("query.input.topic");

        KafkaSource<ScoringRuleCommand> source =
              new FlinkUtils<>(ScoringRuleCommand.class).createKafkaGenericSource(topic, params,
                    SCORING_RULES_GROUP_ID);

        return env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Rule Source")
              .uid("kafka.input.rule.command");
    }

    @Override
    protected void writeQueryResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results) {
        String topic = params.getRequired("query.output.topic");
        KafkaSink<ScoringRuleCommandResult> sink =
              new FlinkUtils<>(ScoringRuleCommandResult.class).createKafkaSink(topic, SCORING_RULES_GROUP_ID, params);
        results.sinkTo(sink).name("Kafka Rule Command Results").uid("kafka.output.rule.command.results");
    }

    public static String scoringSummationName() {
        return SCORING_SUMMARIZATION;
    }
}
