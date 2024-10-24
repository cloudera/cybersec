/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber;

import static java.util.stream.Collectors.toMap;

import com.google.common.base.Joiner;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageUtils {

    public static final String MESSAGE_FIELD_DELIMITER = ".";

    /**
     * Returns a new message combining the fields from the first message with the fields passed in.
     *
     * @param message Original message.
     * @param field   Fields to add to the output message.
     * @return Message with fields from the original message and the fields passed in.
     *       If fields is empty, return original unmodified message.
     */
    public static Message addFields(Message message, Map<String, String> field) {
        if (field != null && !field.isEmpty()) {
            return message.toBuilder()
                  .extensions(Stream.concat(
                        streamExtensions(message),
                        field.entrySet().stream()
                  ).collect(toMap(Map.Entry::getKey, Map.Entry::getValue, MessageUtils::merge)))
                  .build();
        } else {
            return message;
        }
    }

    public static Message replaceFields(Message message, Map<String, String> fields) {
        if (fields != null && !fields.isEmpty()) {
            Map<String, String> messageExtensions =
                  message.getExtensions() != null ? message.getExtensions() : new HashMap<>();
            Map<String, String> newMessageExtensions = new HashMap<>(messageExtensions);
            newMessageExtensions.putAll(fields);

            return message.toBuilder().extensions(newMessageExtensions).build();
        } else {
            return message;
        }
    }

    private static Stream<Map.Entry<String, String>> prefixMap(Map<String, String> field, String prefix) {
        return field.entrySet().stream().collect(
              toMap(k -> Joiner.on(MESSAGE_FIELD_DELIMITER).skipNulls().join(prefix, k.getKey()), Map.Entry::getValue,
                    MessageUtils::merge)).entrySet().stream();
    }

    public static Message enrich(Message message, Map<String, String> enrichmentExtensions,
                                 List<DataQualityMessage> dataQualityMessages) {
        return enrich(message, enrichmentExtensions, null, dataQualityMessages);
    }

    public static Message enrich(Message message, Map<String, String> enrichmentExtensions, String prefix,
                                 List<DataQualityMessage> dataQualityMessages) {
        if (!enrichmentExtensions.isEmpty() || !dataQualityMessages.isEmpty()) {
            return message.toBuilder()
                  .extensions(Stream.concat(
                        streamExtensions(message),
                        prefixMap(enrichmentExtensions, prefix)
                  ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, MessageUtils::merge)))
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

    public static String merge(String str1, String str2) {
        return str2;
    }

    public static void addQualityMessage(List<DataQualityMessage> messages, DataQualityMessageLevel level,
                                         String errorMessage, String fieldName, String feature) {
        Optional<DataQualityMessage> duplicate = messages.stream()
              .filter(m -> m.getLevel().equals(level.name()) && m.getMessage().equals(errorMessage)).findFirst();
        if (!duplicate.isPresent()) {
            messages.add(DataQualityMessage.builder()
                  .level(level.name())
                  .feature(feature)
                  .field(fieldName)
                  .message(errorMessage).build());
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
