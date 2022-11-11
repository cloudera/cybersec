package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.hbase.LookupKey;

public interface EnrichmentLookupBuilder {
    LookupKey build(EnrichmentStorageConfig storageConfig, String enrichmentType, String fieldValue);
}
