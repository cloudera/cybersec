package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.hbase.LookupKey;
import org.apache.commons.lang3.StringUtils;

public class SimpleLookupKeyBuilder implements EnrichmentLookupBuilder {

    @Override
    public LookupKey build(EnrichmentStorageConfig storageConfig, String enrichmentType, String fieldValue) {
        String columnFamily = StringUtils.isEmpty(storageConfig.getColumnFamily()) ? enrichmentType : storageConfig.getColumnFamily();
        return SimpleLookupKey.builder().tableName(storageConfig.getHbaseTableName()).
                cf(columnFamily).key(fieldValue).build();
    }
}
