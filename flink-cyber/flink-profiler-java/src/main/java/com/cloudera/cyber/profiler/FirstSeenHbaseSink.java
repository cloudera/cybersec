package com.cloudera.cyber.profiler;

import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.hbase.AbstractHbaseSinkFunction;
import org.apache.flink.api.java.utils.ParameterTool;


public class FirstSeenHbaseSink extends AbstractHbaseSinkFunction<ProfileMessage> {

    public FirstSeenHbaseSink(EnrichmentStorageConfig enrichmentStorageConfig, ProfileGroupConfig profileGroupConfig, ParameterTool params) {
        super(enrichmentStorageConfig.getHbaseTableName(), new FirstSeenHbaseMutationConverter(enrichmentStorageConfig, profileGroupConfig), params, "numFirstSeen");
    }

}
