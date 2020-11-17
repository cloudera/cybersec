package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import org.apache.flink.api.common.functions.FilterFunction;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class FilterEnrichmentType implements FilterFunction<EnrichmentEntry> {
    private final List<EnrichmentConfig> enrichmentEntries;

    public FilterEnrichmentType(List<EnrichmentConfig> enrichmentConfigStream, EnrichmentKind kind) {
        this.enrichmentEntries = enrichmentConfigStream.stream()
                .filter(e -> e.getKind().equals(kind))
                .collect(toList());
    }

    @Override
    public boolean filter(EnrichmentEntry enrichmentEntry) throws Exception {
        return enrichmentEntries.contains(enrichmentEntry);
    }
}
