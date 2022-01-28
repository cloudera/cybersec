package com.cloudera.cyber.enrichment;


import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class Enrichment {
    public static final String DELIMITER = ".";

    protected final String fieldName;
    protected final String feature;
    protected final String prefix;


    public Enrichment(String fieldName, String feature) {
        this.fieldName = fieldName;
        this.feature = feature;
        this.prefix = String.join(DELIMITER, fieldName, feature);
    }

    protected String getName(String enrichmentName) {
        return String.join(DELIMITER, prefix, enrichmentName);
    }

    public List<DataQualityMessage> addQualityMessage(List<DataQualityMessage> messages, DataQualityMessageLevel level, String message) {
        Optional<DataQualityMessage> duplicate = messages.stream().
                filter(m -> m.getLevel().equals(level.name()) && m.getField().equals(fieldName) && m.getFeature().equals(feature) && m.getMessage().equals(message)).findFirst();
        if (!duplicate.isPresent()) {
            messages.add(DataQualityMessage.builder()
                    .level(level.name())
                    .feature(feature)
                    .field(fieldName)
                    .message(message).build());
        }
        return messages;
    }

    public abstract void enrich(Map<String, String> extensions, String enrichmentName, Object enrichmentValue);
}