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

package com.cloudera.cyber.dedupe;

import com.cloudera.cyber.DedupeMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.AssignerWithPeriodicWatermarks;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.util.Preconditions;

import java.util.Arrays;
import java.util.List;

import static com.cloudera.cyber.flink.FlinkUtils.createKafkaSource;

public class DedupeJobKafka extends DedupeJob {
    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);;
        new DedupeJobKafka()
            .createPipeline(params)
            .execute("Flink Deduplicator");
    }

    /**
     * Returns a consumer group id for the deduper ensuring that each topic is only processed once with the same keys
     *
     * @param inputTopic topic to read from
     * @param sessionKey the keys being used to sessionise
     * @return Generated group id for Kafka
     */
    private String createGroupId(String inputTopic, List<String> sessionKey, long sessionTimeout) {
        List<String> parts = Arrays.asList("dedupe",
                inputTopic,
                String.valueOf(sessionKey.hashCode()),
                String.valueOf(sessionTimeout));
        return String.join(".", parts);
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<DedupeMessage> results) {
        FlinkKafkaProducer<DedupeMessage> sink = new FlinkUtils<>(DedupeMessage.class).createKafkaSink(
                params.getRequired("topic.enrichment"),
                "dedup",
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results");
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> sessionKey, Long sessionTimeout) {
        String inputTopic = params.getRequired("topic.input");
        String groupId = createGroupId(inputTopic, sessionKey, sessionTimeout);
        Time allowedLateness = Time.milliseconds(params.getLong(PARAM_DEDUPE_LATENESS, 0L));
        AssignerWithPeriodicWatermarks<Message> assigner = new BoundedOutOfOrdernessTimestampExtractor<Message>(allowedLateness) {
            @Override
            public long extractTimestamp(Message message) {
                return message.getTs();
            }
        };
        return env.addSource(createKafkaSource(inputTopic,
                        params,
                        groupId))
                .assignTimestampsAndWatermarks(assigner)
                        .name("Kafka Source")
                        .uid("kafka.input");
    }
}
