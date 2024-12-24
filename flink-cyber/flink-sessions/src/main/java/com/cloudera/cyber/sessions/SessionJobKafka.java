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

package com.cloudera.cyber.sessions;

import static com.cloudera.cyber.flink.FlinkUtils.createKafkaSource;

import com.cloudera.cyber.GroupedMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import java.util.Arrays;
import java.util.List;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

public class SessionJobKafka extends SessionJob {

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        StreamExecutionEnvironment env = new SessionJobKafka().createPipeline(params);
        env.execute("Flink Sessionizer");
    }

    /**
     * Returns a consumer group id for the sessioniser ensuring that each topic is only processed once with the same keys.
     *
     * @param inputTopic     topic to read from
     * @param sessionKey     the keys being used to sessionise
     * @param sessionTimeout duration of time window for session
     * @return Generated group id for Kakfa
     */
    private String createGroupId(String inputTopic, List<String> sessionKey, Long sessionTimeout) {
        List<String> parts = Arrays.asList("sessionizer",
              inputTopic,
              String.valueOf(sessionKey.hashCode()),
              String.valueOf(sessionTimeout));
        return String.join(".", parts);
    }

    @Override
    protected void writeResults(ParameterTool params, SingleOutputStreamOperator<GroupedMessage> results) {
        KafkaSink<GroupedMessage> sink = new FlinkUtils<>(GroupedMessage.class).createKafkaSink(
              params.getRequired("topic.enrichment"), "sessionizer",
              params);
        results.sinkTo(sink).name("Kafka Results").uid("kafka.results");
    }

    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params,
                                               List<String> sessionKey, Long sessionTimeout) {
        String inputTopic = params.getRequired("topic.input");
        String groupId = createGroupId(inputTopic, sessionKey, sessionTimeout);
        return
              env.fromSource(createKafkaSource(inputTopic,
                       params,
                       groupId), WatermarkStrategy.noWatermarks(), "Kafka Source")
                 .uid("kafka.input");
    }
}
