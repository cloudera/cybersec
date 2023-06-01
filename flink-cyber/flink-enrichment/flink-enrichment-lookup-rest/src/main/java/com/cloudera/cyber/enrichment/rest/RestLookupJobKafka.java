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

package com.cloudera.cyber.enrichment.rest;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;

public class RestLookupJobKafka extends RestLookupJob {

    private static final String DEFAULT_GROUP_ID = "enrichment-rest";
    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        new RestLookupJobKafka().createPipeline(Utils.getParamToolsFromProperties(args)).execute("Enrichment - REST");
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.fromSource(FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, DEFAULT_GROUP_ID),
                WatermarkStrategy.noWatermarks(), "Kafka Source").uid("kafka-source");
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results) {
        results.sinkTo(new FlinkUtils<>(Message.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), DEFAULT_GROUP_ID, params))
                .name("Kafka Sink").uid("kafka-sink");
    }
}
