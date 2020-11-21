package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.IOException;
import java.util.List;

@Slf4j
public abstract class HbaseJobRaw extends HbaseJob {
    public static DataStream<Message> enrich(DataStream<Message> source, StreamExecutionEnvironment env, List<EnrichmentConfig> configs) throws IOException {
        return source.map(new HbaseEnrichmentMapFunction(configs, "enrichments"))
                .name("HBase Enrichment Mapper").uid("hbase-map");
    }
}
