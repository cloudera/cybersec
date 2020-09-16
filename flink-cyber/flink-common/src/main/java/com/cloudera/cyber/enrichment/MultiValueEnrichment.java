package com.cloudera.cyber.enrichment;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MultiValueEnrichment extends Enrichment {

    public MultiValueEnrichment(String fieldName, String feature) {
        super(fieldName, feature);
    }

    @Override
    public void enrich(Map<String, Object> extensions, String enrichmentName, Object enrichmentValue) {
        if (enrichmentValue != null) {
            String extensionName = getName(enrichmentName);
            @SuppressWarnings("unchecked") Set<Object> enrichmentValueSet = (Set<Object>) extensions.get(extensionName);
            if (enrichmentValueSet == null) {
                enrichmentValueSet = new HashSet<>();
                extensions.put(extensionName, enrichmentValueSet);
            }
            enrichmentValueSet.add(enrichmentValue);
        }
    }
}
