package com.cloudera.cyber.enrichment.threatq;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.addons.hbase.HBaseSinkFunction;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.metrics.Counter;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static com.cloudera.cyber.enrichment.ConfigUtils.PARAMS_CONFIG_FILE;

@Slf4j
public abstract class ThreatQJob {
    private static final byte[] cf = Bytes.toBytes("t");
    private static final String tableName = "threatq";

    public static DataStream<Message> enrich(DataStream<Message> source,
                                             List<ThreatQConfig> configs
    ) throws IOException {
        DataStream<Message> pipeline = source.map(new ThreatQHBaseMap(configs)).name("Apply ThreatQ").uid("threatq-enrich");
        return pipeline;
    }

    public static DataStreamSink<ThreatQEntry> ingest(DataStream<ThreatQEntry> enrichmentSource, List<ThreatQConfig> configs) {
        Configuration conf = new Configuration();
        conf.addResource("/etc/hbase/conf/hbase-site.xml");

        return enrichmentSource.addSink(new HBaseSinkFunction<ThreatQEntry>(tableName, HBaseConfiguration.create(conf)) {
            private transient Counter newThreats;

            @Override
            public void open(org.apache.flink.configuration.Configuration parameters) throws Exception {
                super.open(parameters);
                newThreats = getRuntimeContext().getMetricGroup().counter("threatqNew");
            }

            @Override
            public void executeMutations(ThreatQEntry threatQEntry, Context context, BufferedMutator bufferedMutator) throws Exception {
                newThreats.inc();

                Put put = new Put(Bytes.toBytes(threatQEntry.getTq_type() + ":" + threatQEntry.getIndicator()));

                put.addColumn(cf, Bytes.toBytes("id"), Bytes.toBytes(threatQEntry.getTq_id().toString()));
                if (threatQEntry.getTq_sources() != null && threatQEntry.getTq_sources().size() > 0)
                    put.addColumn(cf, Bytes.toBytes("sources"), Bytes.toBytes(String.join(",", threatQEntry.getTq_sources())));
                put.addColumn(cf, Bytes.toBytes("createdAt"), Bytes.toBytes(threatQEntry.getTq_created_at().getTime()));
                put.addColumn(cf, Bytes.toBytes("updatedAt"), Bytes.toBytes(threatQEntry.getTq_updated_at().getTime()));
                put.addColumn(cf, Bytes.toBytes("touchedAt"), Bytes.toBytes(threatQEntry.getTq_touched_at().getTime()));
                put.addColumn(cf, Bytes.toBytes("type"), Bytes.toBytes(threatQEntry.getTq_type()));
                put.addColumn(cf, Bytes.toBytes("savedSearch"), Bytes.toBytes(threatQEntry.getTq_saved_search()));
                put.addColumn(cf, Bytes.toBytes("url"), Bytes.toBytes(threatQEntry.getTq_url()));
                if (threatQEntry.getTq_tags() != null && threatQEntry.getTq_tags().size() > 0)
                    put.addColumn(cf, Bytes.toBytes("tags"), Bytes.toBytes(String.join(",", threatQEntry.getTq_tags())));
                put.addColumn(cf, Bytes.toBytes("status"), Bytes.toBytes(threatQEntry.getTq_status()));
                put.addColumn(cf, Bytes.toBytes("score"), Bytes.toBytes(threatQEntry.getTq_score()));

                threatQEntry.getTq_attributes().forEach((k, v) -> {
                    put.addColumn(cf, Bytes.toBytes(k), Bytes.toBytes(v));
                });

                bufferedMutator.mutate(put);
            }
        }).name("ThreatQ HBase Writer").uid("threatq-hbase");

    }

    public static List<ThreatQConfig> parseConfigs(byte[] configJson) throws IOException {
        return new ObjectMapper().readValue(
                configJson,
                new TypeReference<List<ThreatQConfig>>() {
                });
    }

    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        DataStream<Message> source = createSource(env, params);
        DataStream<ThreatQEntry> enrichmentSource = createEnrichmentSource(env, params);

        byte[] configJson = Files.readAllBytes(Paths.get(params.getRequired(PARAMS_CONFIG_FILE)));
        List<ThreatQConfig> configs = parseConfigs(configJson);
        log.info("ThreatQ Configs {}", configs);

        ingest(enrichmentSource, configs);

        DataStream<Message> pipeline = enrich(source,configs);
        writeResults(env, params, pipeline);
        return env;
    }

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> reduction);

    public abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<ThreatQEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);


}
