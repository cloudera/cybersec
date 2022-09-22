package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import org.apache.flink.api.common.functions.FilterFunction;

import java.util.ArrayList;
import java.util.List;

public class FilterEnrichmentType implements FilterFunction<EnrichmentCommand> {
    private final List<String> enrichmentTypes = new ArrayList<>();

    public FilterEnrichmentType(List<EnrichmentConfig> enrichmentConfigStream, EnrichmentKind kind) {
        for(EnrichmentConfig enrichmentConfig : enrichmentConfigStream) {
            if (enrichmentConfig.getKind().equals(kind)) {
                for(EnrichmentField field : enrichmentConfig.getFields()) {
                    enrichmentTypes.add(field.getEnrichmentType());
                }
            }
        }
    }

    @Override
    public boolean filter(EnrichmentCommand enrichmentCommand) {
        return enrichmentTypes.contains(enrichmentCommand.getPayload().getType());
    }
}
