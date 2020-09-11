package com.cloudera.cyber;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

public class MessageUtils {
    public static Message addFields(Message message, Map<String, String> field) {
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
}
