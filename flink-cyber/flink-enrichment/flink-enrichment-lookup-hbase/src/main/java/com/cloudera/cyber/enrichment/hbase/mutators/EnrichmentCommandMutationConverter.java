package com.cloudera.cyber.enrichment.hbase.mutators;

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import org.apache.hadoop.hbase.client.Mutation;

import java.io.Serializable;

public interface EnrichmentCommandMutationConverter extends Serializable {
    Mutation convertToMutation(EnrichmentStorageConfig storageConfig, EnrichmentCommand enrichmentCommand);
}
