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

package com.cloudera.cyber.indexing;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

@Slf4j
public class SolrJobKafka extends SolrJob {
    private static final String DEFAULT_TOPIC_CONFIG_LOG = "solr.config.log";


    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        new SolrJobKafka().createPipeline(Utils.getParamToolsFromProperties(args)).execute("Indexing - Solr");
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.fromSource(
              FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, "indexer-solr"),
              WatermarkStrategy.noWatermarks(), "Message Source").uid("message-source");
    }

    protected DataStream<CollectionField> createConfigSource(StreamExecutionEnvironment env, ParameterTool params) {
        DataStreamSource<CollectionField> dataStreamSource = env.addSource(
              new SolrCollectionFieldsSource(Arrays.asList(params.getRequired("solr.urls").split(",")),
                    params.getLong(PARAMS_SCHEMA_REFRESH_INTERVAL, DEFAULT_SCHEMA_REFRESH_INTERVAL))
        );
        return dataStreamSource
              .name("Schema Stream").uid("schema-stream")
              .setMaxParallelism(1)
              .setParallelism(1);
    }

    @Override
    protected void logConfig(DataStream<CollectionField> configSource, ParameterTool params) {
        String topic = params.get(PARAMS_TOPIC_CONFIG_LOG, DEFAULT_TOPIC_CONFIG_LOG);

        KafkaSink<CollectionField> sink =
              new FlinkUtils<>(CollectionField.class).createKafkaSink(topic, "indexer-solr-schema", params);

        configSource.sinkTo(sink).name("Schema Log").uid("schema-log")
              .setParallelism(1);
    }
}
