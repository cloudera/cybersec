package com.cloudera.cyber.enrichment;

import com.google.common.base.Joiner;

import java.util.Map;

public class MetronGeoEnrichment extends Enrichment {
    public MetronGeoEnrichment(String fieldName, String feature) {
        super(fieldName, feature);
    }

    @Override
    protected String getName(String enrichmentName) {
        return Joiner.on(DELIMITER).skipNulls().join(this.fieldName, enrichmentName);
    }

    @Override
    public void enrich(Map<String, String> extensions, String enrichmentName, Object enrichmentValue) {
        if (enrichmentValue != null) {
            String extensionName =getName(enrichmentName);
            extensions.put(extensionName, enrichmentValue.toString());
        }

    }
}
