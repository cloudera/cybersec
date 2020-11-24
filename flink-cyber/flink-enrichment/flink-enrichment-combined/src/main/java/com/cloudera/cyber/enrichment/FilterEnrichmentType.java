package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import org.apache.flink.api.common.functions.FilterFunction;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class FilterEnrichmentType implements FilterFunction<EnrichmentCommand> {
    private final List<String> enrichmentTypes;

    public FilterEnrichmentType(List<EnrichmentConfig> enrichmentConfigStream, EnrichmentKind kind) {
        this.enrichmentTypes =  enrichmentConfigStream.stream()
                .filter(e -> e.getKind().equals(kind))
                .map(e -> e.getType())
                .collect(toList());
    }

    @Override
    public boolean filter(EnrichmentCommand enrichmentCommand) {
        return enrichmentTypes.contains(enrichmentCommand.getPayload().getType());
    }
}
