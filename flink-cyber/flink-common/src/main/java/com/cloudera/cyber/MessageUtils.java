package com.cloudera.cyber;

import com.cloudera.cyber.data.quality.DataQualityMessage;
import com.cloudera.cyber.data.quality.MessageLevel;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.time.Instant;
import java.util.*;

import static java.util.stream.Collectors.toMap;

public class MessageUtils {
    public static Message addFields(Message message, Map<String, Object> field) {
        return Message.newBuilder(message)
                .setExtensions(Stream.concat(
                        message.getExtensions().entrySet().stream(),
                        field.entrySet().stream()
                ).collect(toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();
    }

    public static Message addFields(Message message, Map<String, String> field, String prefix) {
        return Message.newBuilder(message)
                .setExtensions(Stream.concat(
                        message.getExtensions().entrySet().stream(),
                        prefixMap(field, prefix)
                ).collect(toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();
    }

    private static Stream<Map.Entry<String,String>> prefixMap(Map<String, String> field, String prefix) {
        return field.entrySet().stream().collect(toMap(k -> prefix + k, Map.Entry::getValue)).entrySet().stream();
    }

    public static Message reportQualityMessage(Message message, MessageLevel level, String fieldName, String feature, String messageText) {
        List<DataQualityMessage> dataQualityMessages = message.getDataQualityMessages();
        if (dataQualityMessages == null) {
            dataQualityMessages = new ArrayList<>();
            message.setDataQualityMessages(dataQualityMessages);
        }
        dataQualityMessages.add(new DataQualityMessage(level, feature, fieldName, messageText));

        return message;
    }

    public static Message enrich(Message message, String fieldName, String feature, String enrichment, Object value) {
        if (value != null) {
            message.getExtensions().put(getEnrichmentFieldName(fieldName, feature, enrichment), value);
        }
        return message;
    }

    public static Message enrichSet(Message message, String field, String feature, String enrichmentName, Object elementValue) {
        if (elementValue != null) {
            String enrichedFieldName = getEnrichmentFieldName(field, feature, enrichmentName);
            Map<String, Object> extensions = message.getExtensions();
            // noinspection unchecked
            Set<Object> enrichmentValueSet = (Set<Object>) extensions.get(enrichedFieldName);
            if (enrichmentValueSet == null) {
                enrichmentValueSet = new HashSet<>();
                extensions.put(enrichedFieldName, enrichmentValueSet);
            }
            enrichmentValueSet.add(elementValue);
        }
        return message;
    }

    public static long getCurrentTimestamp() {
        return Instant.now().toEpochMilli();
    }

    private static String getEnrichmentFieldName(String fieldName, String feature, String enrichment) {
        return String.join(".", fieldName, feature, enrichment);
    }
}