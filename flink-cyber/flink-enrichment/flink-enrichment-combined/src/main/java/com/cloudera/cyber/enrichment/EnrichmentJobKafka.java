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

package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.enrichment.threatq.ThreatQParserFlatMap;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.ScoringRuleCommand;
import com.cloudera.cyber.scoring.ScoringRuleCommandResult;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import java.util.Properties;

import static com.cloudera.cyber.enrichment.lookup.LookupJobKafka.PARAMS_QUERY_OUTPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;

public class EnrichmentJobKafka extends EnrichmentJob {

    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";
    private static final String PARAMS_GROUP_ID = "group.id";
    private static final String DEFAULT_GROUP_ID = "enrichment-combined";
    public static final String SCORING_RULES_GROUP_ID = "scoring-rules";
    private static final String PARAMS_TOPIC_THREATQ_INPUT = "threatq.topic.input";


    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        FlinkUtils.executeEnv(new EnrichmentJobKafka().createPipeline(params),"Triaging Job - default",params);
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<ScoredMessage> reduction) {
        reduction.sinkTo(new FlinkUtils<>(ScoredMessage.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), "enrichments-combined", params))
                .name("Kafka Triaging Scored Sink").uid("kafka-sink");
    }

    @Override
    public SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.fromSource(
                FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT),
                        params, params.get(PARAMS_GROUP_ID, DEFAULT_GROUP_ID)),
                WatermarkStrategy.noWatermarks(), "Kafka Source").uid("kafka-source");
    }

    @Override
    protected DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        KafkaSource<EnrichmentCommand> source = new FlinkUtils<>(EnrichmentCommand.class).createKafkaGenericSource(params.getRequired(PARAMS_TOPIC_ENRICHMENT_INPUT), params, params.get(PARAMS_GROUP_ID, DEFAULT_GROUP_ID));
        return env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Enrichments").uid("kafka-enrichment-source");
    }

    @Override
    protected void writeEnrichmentQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> sideOutput) {
        sideOutput.sinkTo(new FlinkUtils<>(EnrichmentCommandResponse.class).createKafkaSink(params.getRequired(PARAMS_QUERY_OUTPUT), "enrichment-combined-command", params))
                .name("Triaging Query Sink").uid("kafka-enrichment-query-sink");
    }

    @Override
    protected DataStream<EnrichmentCommand> createThreatQSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired(PARAMS_TOPIC_THREATQ_INPUT);
        String groupId = "threatq-parser";

        Properties kafkaProperties = readKafkaProperties(params, groupId, true);

        return env.fromSource(FlinkUtils.createKafkaStringSource(topic, kafkaProperties), WatermarkStrategy.noWatermarks(),"ThreatQ Source").uid("threatq-kafka")
                .flatMap(new ThreatQParserFlatMap()).name("ThreatQ Parser").uid("threatq-parser");
    }

    @Override
    protected DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired("query.input.topic");
        KafkaSource<ScoringRuleCommand> source = new FlinkUtils<>(ScoringRuleCommand.class).createKafkaGenericSource(topic, params, SCORING_RULES_GROUP_ID);
        return env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Score Rule Source").uid("kafka.input.rule.command");
    }

    @Override
    protected void writeScoredRuleCommandResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results) {
        String topic = params.getRequired("query.output.topic");
        KafkaSink<ScoringRuleCommandResult> sink = new FlinkUtils<>(ScoringRuleCommandResult.class).createKafkaSink(topic, SCORING_RULES_GROUP_ID, params);
        results.sinkTo(sink).name("Kafka Score Rule Command Results").uid("kafka.output.rule.command.results");
    }


}
