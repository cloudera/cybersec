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

package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.scoring.ScoredMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;

@Slf4j
public class HiveJobKafka extends HiveJob {

    private static final String PARAMS_GROUP_ID = "kafka.group.id";
    private static final String DEFAULT_GROUP_ID = "indexer-hive";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        final StreamExecutionEnvironment pipeline = new HiveJobKafka().createPipeline(params);
        if (pipeline != null) {
            FlinkUtils.executeEnv(pipeline, "Indexing - Hive", params);
        }
    }

    @Override
    protected SingleOutputStreamOperator<ScoredMessage> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.fromSource(
                new FlinkUtils<>(ScoredMessage.class).createKafkaGenericSource(params.getRequired(PARAMS_TOPIC_INPUT), params, params.get(PARAMS_GROUP_ID, DEFAULT_GROUP_ID)),
                WatermarkStrategy.noWatermarks(), "Kafka Source").
                uid("kafka-source");
    }
}
