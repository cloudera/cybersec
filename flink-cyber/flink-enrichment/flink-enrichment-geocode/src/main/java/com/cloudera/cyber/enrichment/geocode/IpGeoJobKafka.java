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

package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer;
import org.apache.flink.util.Preconditions;

import java.util.Arrays;
import java.util.List;

public class IpGeoJobKafka extends IpGeoJob {
    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        new IpGeoJobKafka()
                .createPipeline(params)
                .execute("Flink IP Geocode");
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<Message> results) {
        FlinkKafkaProducer<Message> sink = new FlinkUtils<>(Message.class).createKafkaSink(
                params.getRequired("topic.output"),
                "enrichment-geocode",
                params);
        results.addSink(sink).name("Kafka Results").uid("kafka.results");
    }

    /**
     * Returns a consumer group id for the geocoder ensuring that each topic is only processed once with the same fields
     *
     * @param inputTopic topic to read from
     * @param ipFields the ip fields to be geocoded
     * @return Kafka group id for geocoder
     */
    private String createGroupId(String inputTopic, List<String> ipFields) {
        List<String> parts = Arrays.asList("ipgeo",
                inputTopic,
                String.valueOf(ipFields.hashCode()));
        return String.join(".", parts);
    }

    @Override
    protected SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params, List<String> ipFields) {
        String inputTopic = params.getRequired("topic.input");
      return env.addSource(FlinkUtils.createKafkaSource(inputTopic,
                params,createGroupId(inputTopic, ipFields)))
                .name("Kafka Source")
                .uid("kafka.input");
    }

}
