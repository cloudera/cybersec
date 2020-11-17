package com.cloudera.cyber.enrichment;

import java.util.Map;

public class SingleValueEnrichment extends Enrichment {
    public SingleValueEnrichment(String fieldName, String feature) {
        super(fieldName, feature);
    }

    @Override
    public void enrich(Map<String, String> extensions, String enrichmentName, Object enrichmentValue) {
        if (enrichmentValue != null) {
            String extensionName = getName(enrichmentName);
            extensions.put(extensionName, enrichmentValue.toString());
        }
    }
}
