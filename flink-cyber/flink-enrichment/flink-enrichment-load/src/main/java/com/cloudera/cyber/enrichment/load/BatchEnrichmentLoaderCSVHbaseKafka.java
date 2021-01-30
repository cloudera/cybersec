package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.HBaseEnrichmentCommandSink;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.addons.hbase.HBaseSinkFunction;
import org.apache.flink.addons.hbase.HBaseWriteOptions;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

public class BatchEnrichmentLoaderCSVHbaseKafka extends BatchEnrichmentLoaderCSV {
    protected static final String PARAMS_ENRICHMENT_TABLE = "enrichments.table";
    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length == 1, "Arguments must consist of a single properties file");
        new BatchEnrichmentLoaderCSVHbaseKafka().runPipeline(ParameterTool.fromPropertiesFile(args[0])).execute("Batch Enrichment Load");
    }

    @Override
    protected void writeEnrichments(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentCommand> enrichmentSource) {
        String topic = params.get(PARAMS_TOPIC_ENRICHMENT_INPUT);
        String enrichmentTable = params.get(PARAMS_ENRICHMENT_TABLE);
        Preconditions.checkArgument(topic != null || enrichmentTable != null, "Properties must specify a topic %s or an enrichment table %s. Define one of these properties.", PARAMS_TOPIC_ENRICHMENT_INPUT, PARAMS_ENRICHMENT_TABLE);
        Preconditions.checkArgument((topic == null) || (enrichmentTable == null), "Properties can not specify both a topic %s or an enrichment table %s. Remove one of these properties.", PARAMS_TOPIC_ENRICHMENT_INPUT, PARAMS_ENRICHMENT_TABLE);
        if (enrichmentTable != null) {
            HBaseSinkFunction<EnrichmentCommand> hbaseSink = new HBaseEnrichmentCommandSink(params.getRequired(PARAMS_ENRICHMENT_TABLE));
            hbaseSink.setWriteOptions(HBaseWriteOptions.builder()
                    .setBufferFlushIntervalMillis(1000)
                    .build()
            );
            enrichmentSource.addSink(hbaseSink).name("HBase Enrichment Command Sink");
        } else {
            enrichmentSource.addSink(new FlinkUtils<>(EnrichmentCommand.class).createKafkaSink(topic, "enrichment_loader", params)).name("Kafka Enrichment Command Sink");
        }
    }
}
