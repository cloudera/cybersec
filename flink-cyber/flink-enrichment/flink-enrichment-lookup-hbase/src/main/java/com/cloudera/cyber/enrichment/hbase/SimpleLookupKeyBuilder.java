package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.hbase.LookupKey;

public class SimpleLookupKeyBuilder implements EnrichmentLookupBuilder {

    @Override
    public LookupKey build(EnrichmentStorageConfig storageConfig, String enrichmentType, String fieldValue) {
        return SimpleLookupKey.builder().tableName(storageConfig.getHbaseTableName()).
                cf(enrichmentType).key(fieldValue).build();
    }
}
