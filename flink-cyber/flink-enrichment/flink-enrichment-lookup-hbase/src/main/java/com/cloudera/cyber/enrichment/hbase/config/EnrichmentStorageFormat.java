package com.cloudera.cyber.enrichment.hbase.config;

import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.hbase.mutators.EnrichmentCommandMutationConverter;
import com.cloudera.cyber.enrichment.hbase.mutators.MetronHbaseEnrichmentMutationConverter;
import com.cloudera.cyber.enrichment.hbase.mutators.SimpleHbaseEnrichmentMutationConverter;

public enum EnrichmentStorageFormat {
    HBASE_METRON(new MetronHbaseEnrichmentMutationConverter()),
    HBASE_SIMPLE(new SimpleHbaseEnrichmentMutationConverter());

    private final EnrichmentCommandMutationConverter mutationConverter;

    EnrichmentStorageFormat(EnrichmentCommandMutationConverter mutationConverter) {
        this.mutationConverter = mutationConverter;
    }

    public EnrichmentCommandMutationConverter getMutationConverter() {
        return mutationConverter;
    }
}
