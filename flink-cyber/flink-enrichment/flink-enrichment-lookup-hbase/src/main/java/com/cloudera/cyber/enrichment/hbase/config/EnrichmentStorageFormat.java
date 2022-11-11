package com.cloudera.cyber.enrichment.hbase.config;

import com.cloudera.cyber.enrichment.hbase.EnrichmentLookupBuilder;
import com.cloudera.cyber.enrichment.hbase.MetronLookupKeyBuilder;
import com.cloudera.cyber.enrichment.hbase.SimpleLookupKeyBuilder;
import com.cloudera.cyber.enrichment.hbase.mutators.EnrichmentCommandMutationConverter;
import com.cloudera.cyber.enrichment.hbase.mutators.MetronHbaseEnrichmentMutationConverter;
import com.cloudera.cyber.enrichment.hbase.mutators.SimpleHbaseEnrichmentMutationConverter;

public enum EnrichmentStorageFormat {
    HBASE_METRON(new MetronHbaseEnrichmentMutationConverter(), new MetronLookupKeyBuilder()),
    HBASE_SIMPLE(new SimpleHbaseEnrichmentMutationConverter(), new SimpleLookupKeyBuilder());

    private final EnrichmentCommandMutationConverter mutationConverter;
    private final EnrichmentLookupBuilder lookupBuilder;

    EnrichmentStorageFormat(EnrichmentCommandMutationConverter mutationConverter, EnrichmentLookupBuilder lookupBuilder) {
        this.mutationConverter = mutationConverter;
        this.lookupBuilder = lookupBuilder;
    }

    public EnrichmentCommandMutationConverter getMutationConverter() {
        return mutationConverter;
    }

    public EnrichmentLookupBuilder getLookupBuilder() { return lookupBuilder; }

}
