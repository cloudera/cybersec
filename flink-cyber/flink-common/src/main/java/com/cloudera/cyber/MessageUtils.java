package com.cloudera.cyber;

import com.google.common.base.Joiner;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class MessageUtils {

    public static final String MESSAGE_FIELD_DELIMITER = ".";
    /**
     * Returns a new message combining the fields from the first message with the fields passed in.
     *
     * @param message Original message.
     * @param field   Fields to add to the output message.
     * @return Message with fields from the original message and the fields passed in.  If fields is empty, return original unmodified message.
     */
    public static Message addFields(Message message, Map<String, String> field) {
        if (field != null && !field.isEmpty()) {
            return message.toBuilder()
                    .extensions(Stream.concat(
                            streamExtensions(message),
                            field.entrySet().stream()
                    ).collect(toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .build();
        } else {
            return message;
        }
    }

    public static Message replaceFields(Message message, Map<String, String> fields) {
        if (fields != null && !fields.isEmpty()) {
            Map<String, String> messageExtensions = message.getExtensions() != null ? message.getExtensions() : new HashMap<>();
            Map<String, String> newMessageExtensions = new HashMap<>(messageExtensions);
            newMessageExtensions.putAll(fields);

            return message.toBuilder().extensions(newMessageExtensions).build();
        } else {
            return message;
        }
    }

    private static Stream<Map.Entry<String,String>> prefixMap(Map<String, String> field, String prefix) {
        return field.entrySet().stream().collect(toMap(k -> Joiner.on(MESSAGE_FIELD_DELIMITER).skipNulls().join(prefix, k.getKey()), Map.Entry::getValue)).entrySet().stream();
    }

    public static Message enrich(Message message, Map<String, String> enrichmentExtensions, List<DataQualityMessage> dataQualityMessages) {
        return enrich(message, enrichmentExtensions, null, dataQualityMessages);
    }

    public static Message enrich(Message message, Map<String, String> enrichmentExtensions, String prefix, List<DataQualityMessage> dataQualityMessages) {
        if (!enrichmentExtensions.isEmpty() || !dataQualityMessages.isEmpty()) {
            return message.toBuilder()
                    .extensions(Stream.concat(
                            streamExtensions(message),
                            prefixMap(enrichmentExtensions, prefix)
                    ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                    .dataQualityMessages(Stream.concat(
                            streamDataQualityMessages(message),
                            dataQualityMessages.stream()
                    ).distinct().collect(Collectors.toList()))
                    .build();
        } else {
            // no changes to the message needed
            return message;
        }
    }

    public static Stream<Map.Entry<String, String>> streamExtensions(Message message) {
        Map<String, String> extensions = message.getExtensions();
        if (extensions == null) {
            return Stream.empty();
        } else {
            return extensions.entrySet().stream();
        }
    }

    public static Stream<DataQualityMessage> streamDataQualityMessages(Message message) {
        List<DataQualityMessage> dataQualityMessages = message.getDataQualityMessages();
        if (dataQualityMessages == null) {
            return Stream.empty();
        } else {
            return dataQualityMessages.stream();
        }
    }

    public static long getCurrentTimestamp() {
        return Instant.now().toEpochMilli();
    }

}
