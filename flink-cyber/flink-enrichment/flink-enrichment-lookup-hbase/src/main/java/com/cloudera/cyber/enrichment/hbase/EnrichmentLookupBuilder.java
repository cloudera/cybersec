package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.hbase.LookupKey;

import java.io.Serializable;

public interface EnrichmentLookupBuilder extends Serializable {
    LookupKey build(EnrichmentStorageConfig storageConfig, String enrichmentType, String fieldValue);
}
