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

package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.enrichment.hbase.writer.HbaseEnrichmentCommandSink;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.hbase.sink.HBaseSinkFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

@Slf4j
public class BatchEnrichmentLoaderCSVHbaseKafka extends BatchEnrichmentLoaderCSV {
    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        FlinkUtils.executeEnv(new BatchEnrichmentLoaderCSVHbaseKafka().runPipeline(params),
                String.format("Enrichment %s - batch load", params.get(ENRICHMENT_TYPE, "unspecified")),
                params);
    }

    @Override
    protected void writeResults(ParameterTool params, EnrichmentsConfig enrichmentsConfig, String enrichmentType, DataStream<EnrichmentCommand> enrichmentSource, StreamExecutionEnvironment env) {
        String topic = params.get(PARAMS_TOPIC_ENRICHMENT_INPUT);
        if (topic != null) {
            enrichmentSource.sinkTo(new FlinkUtils<>(EnrichmentCommand.class).createKafkaSink(topic, "enrichment_loader", params)).name("Kafka Enrichment Command Sink");
        } else {
            String hbaseTable = enrichmentsConfig.getStorageForEnrichmentType(enrichmentType).getHbaseTableName();
            HBaseSinkFunction<EnrichmentCommand> hbaseSink = new HbaseEnrichmentCommandSink(hbaseTable, enrichmentsConfig, params);
            enrichmentSource.addSink(hbaseSink);
        }
    }
}
