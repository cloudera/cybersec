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

package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.SourcesWithHeaders;
import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;

public class LookupJobKafka extends LookupJob {

    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";
    public static final String PARAMS_QUERY_OUTPUT = "enrichment.topic.query.output";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        new LookupJobKafka().createPipeline(Utils.getParamToolsFromProperties(args)).execute("Enrichments - Local Lookup");
    }

    @Override
    protected void writeQueryResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommandResponse> sideOutput) {
        sideOutput.addSink(new FlinkUtils<>(EnrichmentCommandResponse.class).createKafkaSink(params.getRequired(PARAMS_QUERY_OUTPUT), "enrichments-lookup-command", params))
                .name("Kafka Sink").uid("kafka-sink");
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction) {
        reduction.addSink(new FlinkUtils<>(Message.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), "enrichents-lookup", params))
        .name("Kafka Sink").uid("kafka-sink");
    }

    @Override
    public SingleOutputStreamOperator<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, "enrichment-lookups-local")
        ).name("Kafka Source").uid("kafka-source");
    }

    @Override
    protected DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(new SourcesWithHeaders(EnrichmentCommand.class).createSourceWithHeaders(
                params.getRequired(PARAMS_TOPIC_ENRICHMENT_INPUT),
                params, "enrichment-lookups-local"
                ))
                .name("Kafka Enrichments").uid("kafka-enrichment-source");
    }
}
