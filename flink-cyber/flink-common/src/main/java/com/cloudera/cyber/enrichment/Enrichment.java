package com.cloudera.cyber.enrichment;


import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Enrichment {
    private final String fieldName;
    private final String feature;
    private final String prefix;

    public Enrichment(String fieldName, String feature) {
        this.fieldName = fieldName;
        this.feature = feature;
        this.prefix = String.join(".", fieldName, feature);
    }

    protected String getName(String enrichmentName) {
        return String.join(".", prefix, enrichmentName);
    }

    public void addQualityMessage(List<DataQualityMessage> messages, DataQualityMessageLevel level, String message) {
        Optional<DataQualityMessage> duplicate = messages.stream().
                filter(m -> m.getLevel().equals(level) && m.getField().equals(fieldName) && m.getFeature().equals(feature) && m.getMessage().equals(message)).findFirst();
        if (!duplicate.isPresent()) {
            messages.add(DataQualityMessage.builder()
                    .level(level.name())
                    .feature(feature)
                    .field(fieldName)
                    .message(message).build());
        }
    }

    public abstract void enrich(Map<String, String> extensions, String enrichmentName, Object enrichmentValue);
}