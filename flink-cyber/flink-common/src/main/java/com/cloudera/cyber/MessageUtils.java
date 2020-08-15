package com.cloudera.cyber;

import com.cloudera.cyber.Message;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MessageUtils {
    public static Message addFields(Message message, Map<String, String> field) {
        return Message.newBuilder(message)
                .setExtensions(Stream.concat(
                        message.getExtensions().entrySet().stream(),
                        field.entrySet().stream()
                ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                .build();
    }
}
