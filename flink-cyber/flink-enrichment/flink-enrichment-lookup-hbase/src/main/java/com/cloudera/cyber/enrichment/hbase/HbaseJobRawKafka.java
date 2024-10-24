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

package com.cloudera.cyber.enrichment.hbase;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.enrichment.hbase.writer.HbaseEnrichmentCommandSink;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import java.util.Collections;
import java.util.List;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.hbase.sink.HBaseSinkFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

public class HbaseJobRawKafka extends HbaseJob {
    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";
    public static final String PARAMS_QUERY_OUTPUT = "enrichment.topic.query.output";
    private static final String DEFAULT_GROUP_ID = "enrichment-lookups-hbase";

    public static DataStream<EnrichmentCommandResponse> enrichmentCommandsToHbase(ParameterTool params,
                                                                                  DataStream<EnrichmentCommand> enrichmentSource,
                                                                                  EnrichmentsConfig enrichmentsConfig,
                                                                                  List<String> tables) {
        // filter out commands that don't require changes to Hbase
        DataStream<EnrichmentCommand> hbaseMods = enrichmentSource
              .filter(c -> (c.getType().equals(CommandType.ADD) || c.getType().equals(CommandType.DELETE)))
              .name("ADD-DELETE CommandType Filter");

        // add one sink for each table - hbase table sinks are table specific
        tables.forEach(table -> {
            HBaseSinkFunction<EnrichmentCommand> tableSink =
                  new HbaseEnrichmentCommandSink(table, enrichmentsConfig, params);
            hbaseMods.filter(command -> table.equals(
                           enrichmentsConfig.getStorageForEnrichmentType(command.getPayload().getType()).getHbaseTableName()))
                     .name("Hbase Table ".concat(table).concat(" Filter")).addSink(tableSink)
                     .name("Enrichment ".concat(table).concat(" HBase Sink"));
        });


        return hbaseMods.map(c -> EnrichmentCommandResponse.builder()
                                                           .success(true)
                                                           .message(c.getType().name().concat(" HBase Enrichment"))
                                                           .content(Collections.singletonList(c.getPayload()))
                                                           .headers(c.getHeaders())
                                                           .build());
    }

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        new HbaseJobRawKafka().createPipeline(Utils.getParamToolsFromProperties(args))
                              .execute("Enrichments - HBase Lookup");
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction) {
        reduction.sinkTo(
              new FlinkUtils<>(Message.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), DEFAULT_GROUP_ID,
                    params));
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.fromSource(
              FlinkUtils.createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, DEFAULT_GROUP_ID),
              WatermarkStrategy.noWatermarks(), "Kafka Messages");
    }

    @Override
    protected DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env,
                                                                   ParameterTool params) {
        return env.fromSource(
              new FlinkUtils<>(EnrichmentCommand.class).createKafkaGenericSource(
                    params.getRequired(PARAMS_TOPIC_ENRICHMENT_INPUT), params, DEFAULT_GROUP_ID),
              WatermarkStrategy.noWatermarks(), "Kafka Enrichment Commands");
    }

    @Override
    public DataStream<EnrichmentCommandResponse> writeEnrichments(StreamExecutionEnvironment env, ParameterTool params,
                                                                  DataStream<EnrichmentCommand> enrichmentSource,
                                                                  EnrichmentsConfig enrichmentsConfig) {

        List<String> tables = enrichmentsConfig.getReferencedTables();
        return enrichmentCommandsToHbase(params, enrichmentSource, enrichmentsConfig, tables);
    }

    protected void writeQueryResults(StreamExecutionEnvironment env, ParameterTool params,
                                     DataStream<EnrichmentCommandResponse> enrichmentResults) {
        enrichmentResults.sinkTo(
                               new FlinkUtils<>(EnrichmentCommandResponse.class).createKafkaSink(params.getRequired(PARAMS_QUERY_OUTPUT),
                                     "enrichment-combined-command", params))
                         .name("HBase Enrichment Response Sink").uid("kafka-enrichment-query-sink");
    }

}
