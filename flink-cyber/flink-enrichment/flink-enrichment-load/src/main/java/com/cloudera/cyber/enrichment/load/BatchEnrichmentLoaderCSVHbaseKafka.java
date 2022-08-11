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
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        FlinkUtils.executeEnv(new BatchEnrichmentLoaderCSVHbaseKafka().runPipeline(params),
                String.format("Enrichment %s - batch load", params.get(ENRICHMENT_TYPE, "unspecified")),
                params);
    }

    @Override
    protected void writeResults(ParameterTool params, EnrichmentsConfig enrichmentsConfig, String enrichmentType, DataStream<EnrichmentCommand> enrichmentSource, StreamExecutionEnvironment env) {
        String topic = params.get(PARAMS_TOPIC_ENRICHMENT_INPUT);
        if (topic != null) {
            enrichmentSource.addSink(new FlinkUtils<>(EnrichmentCommand.class).createKafkaSink(topic, "enrichment_loader", params)).name("Kafka Enrichment Command Sink");
        } else {
            String hbaseTable = enrichmentsConfig.getStorageForEnrichmentType(enrichmentType).getHbaseTableName();
            HBaseSinkFunction<EnrichmentCommand> hbaseSink = new HbaseEnrichmentCommandSink(hbaseTable, enrichmentsConfig, params);
            enrichmentSource.addSink(hbaseSink);
        }
    }
}
