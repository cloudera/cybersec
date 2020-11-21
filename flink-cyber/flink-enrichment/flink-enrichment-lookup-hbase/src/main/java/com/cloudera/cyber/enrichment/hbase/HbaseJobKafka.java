package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageTypeFactory;
import com.cloudera.cyber.flink.FlinkUtils;
import org.apache.flink.addons.hbase.HBaseSinkFunction;
import org.apache.flink.addons.hbase.HBaseWriteOptions;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import static com.cloudera.cyber.enrichment.EnrichmentUtils.*;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_OUTPUT;

public class HbaseJobKafka extends HbaseJob {
    private static final String PARAMS_ENRICHMENT_TABLE = "enrichments";
    private static final String PARAMS_TOPIC_ENRICHMENT_INPUT = "enrichment.topic.input";
    private static final String DEFAULT_GROUP_ID = "enrichment-lookups-hbase";

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length == 1, "Arguments must consist of a single properties file");
        new HbaseJobKafka().createPipeline(ParameterTool.fromArgs(args)).execute("Enrichments - HBase Lookup");
    }

    @Override
    protected void writeEnrichments(StreamExecutionEnvironment env, ParameterTool params, DataStream<EnrichmentEntry> enrichmentSource) {
        HBaseSinkFunction<EnrichmentEntry> hbaseSink = new HBaseSinkFunction<EnrichmentEntry>(params.getRequired(PARAMS_ENRICHMENT_TABLE)) {
            @Override
            public void executeMutations(EnrichmentEntry enrichmentEntry, Context context, BufferedMutator mutator) throws Exception {
                // For each incoming query result we create a Put operation
                Put put = new Put(enrichmentKey(enrichmentEntry.getKey()));
                put.addColumn(CF_ID, Q_KEY, Bytes.toBytes(enrichmentEntry.getKey()));
                // add the map for the entries
                enrichmentEntry.getEntries().forEach((k, v) -> put.addColumn(Bytes.toBytes(enrichmentEntry.getType()), Bytes.toBytes(k), Bytes.toBytes(v)));
                mutator.mutate(put);
            }
        };
        hbaseSink.setWriteOptions(HBaseWriteOptions.builder()
                .setBufferFlushIntervalMillis(1000)
                .build()
        );
        enrichmentSource.addSink(hbaseSink);
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction) {
        reduction.addSink(new FlinkUtils<>(Message.class).createKafkaSink(params.getRequired(PARAMS_TOPIC_OUTPUT), params));
    }

    @Override
    public DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils(Message.class).createKafkaSource(params.getRequired(PARAMS_TOPIC_INPUT), params, DEFAULT_GROUP_ID)
        ).returns(new MessageTypeFactory().createTypeInfo(null, null));
    }

    @Override
    protected DataStream<EnrichmentEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.addSource(
                new FlinkUtils(EnrichmentEntry.class).createKafkaGenericSource(params.getRequired(PARAMS_TOPIC_ENRICHMENT_INPUT), params, DEFAULT_GROUP_ID)
        );
    }

}
