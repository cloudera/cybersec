package com.cloudera.cyber;

import com.cloudera.cyber.Message;

import java.util.Map;

public class MessageUtils {
    public static Message addFields(Message message, Map<String, String> field) {
        message.getExtensions().putAll(field);
        return message;
    }
}
