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

package com.cloudera.cyber.enrichment.threatq;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import java.io.ByteArrayInputStream;
import java.util.Properties;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;
import static com.cloudera.cyber.flink.Utils.readKafkaProperties;

public class ThreatQJobKafka extends ThreatQJob {

    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";
    private static final String THREATQ_ENRICHMENT_GROUP_ID = "enrichment-threatq";
    private static final String THREATQ_PARSER_GROUP_ID = "threatq-parser";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        new ThreatQJobKafka().createPipeline(Utils.getParamToolsFromProperties(args)).execute("Enrichments - ThreatQ");
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction) {
        reduction.sinkTo(new FlinkUtils<>(Message.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), THREATQ_ENRICHMENT_GROUP_ID,params))
                .name("Kafka Sink").uid("kafka-sink");
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.fromSource(
                FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, THREATQ_ENRICHMENT_GROUP_ID),
                WatermarkStrategy.noWatermarks(), "Kafka Source").uid("kafka-source");
    }

    @Override
    protected DataStream<ThreatQEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired(PARAMS_TOPIC_ENRICHMENT_INPUT);

        Properties kafkaProperties = readKafkaProperties(params, THREATQ_PARSER_GROUP_ID, true);

        FlatMapFunction<String, ThreatQEntry> threatQParser = (FlatMapFunction<String, ThreatQEntry>) (s, collector) -> ThreatQParser.parse(new ByteArrayInputStream(s.getBytes())).forEach(tq -> collector.collect(tq));

        KafkaSource<String> source = FlinkUtils.createKafkaStringSource(topic, kafkaProperties);
        return env.fromSource(source, WatermarkStrategy.noWatermarks(), "ThreatQ Input").
                uid("kafka-enrichment-source").
                flatMap(threatQParser);
    }
}
