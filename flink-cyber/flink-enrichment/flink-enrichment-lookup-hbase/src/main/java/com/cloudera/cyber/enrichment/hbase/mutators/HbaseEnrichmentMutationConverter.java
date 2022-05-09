package com.cloudera.cyber.enrichment.hbase.mutators;

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import org.apache.flink.connector.hbase.sink.HBaseMutationConverter;
import org.apache.hadoop.hbase.client.Mutation;

public class HbaseEnrichmentMutationConverter implements HBaseMutationConverter<EnrichmentCommand> {
    private final EnrichmentsConfig enrichmentsConfig;

    public HbaseEnrichmentMutationConverter(EnrichmentsConfig enrichmentsConfig) {
        this.enrichmentsConfig = enrichmentsConfig;
    }

    @Override
    public void open() {
    }

    @Override
    public Mutation convertToMutation(EnrichmentCommand enrichmentCommand) {
        EnrichmentStorageConfig storageConfig = enrichmentsConfig.getStorageForEnrichmentType(enrichmentCommand.getPayload().getType());
        EnrichmentCommandMutationConverter mutationConverter = storageConfig.getFormat().getMutationConverter();
        return mutationConverter.convertToMutation(storageConfig, enrichmentCommand);
    }
}
