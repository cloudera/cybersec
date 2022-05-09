package com.cloudera.cyber.enrichment.hbase.mutators;

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;

import java.util.HashMap;

public class MetronHbaseEnrichmentMutationConverter implements EnrichmentCommandMutationConverter {
    private final EnrichmentConverter converter  = new EnrichmentConverter();

    @Override
    public Mutation convertToMutation(EnrichmentStorageConfig storageConfig, EnrichmentCommand enrichmentCommand) {

        EnrichmentKey key = new EnrichmentKey(enrichmentCommand.getPayload().getType(), enrichmentCommand.getPayload().getKey());
        EnrichmentValue value = new EnrichmentValue(new HashMap<>(enrichmentCommand.getPayload().getEntries()));

        return converter.toPut(storageConfig.getColumnFamily(), key, value);
    }

}
