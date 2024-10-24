package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.hbase.LookupKey;

public class MetronLookupKeyBuilder implements EnrichmentLookupBuilder {
    @Override
    public LookupKey build(EnrichmentStorageConfig storageConfig, String enrichmentType, String fieldValue) {
        return MetronLookupKey.builder().tableName(storageConfig.getHbaseTableName())
                              .cf(storageConfig.getColumnFamily())
                              .enrichmentType(enrichmentType).key(fieldValue).build();
    }
}
