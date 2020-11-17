package com.cloudera.cyber;

import com.cloudera.cyber.parser.MessageToParse;

import java.util.Map;

public class TestUtils {

    public static SignedSourceKey source(String topic, int partition, long offset) {
        return SignedSourceKey.builder()
                .topic(topic)
                .partition(partition)
                .offset(offset)
                .signature(new byte[128])
                .build();
    }

    public static SignedSourceKey source() {
        return source("test", 0, 0);
    }

    public static Message createMessage(Map<String, String> extensions) {
        return Message.builder()
                .ts(MessageUtils.getCurrentTimestamp())
                .source("test")
                .extensions(extensions)
                .message("")
                .originalSource(
                        source("topic", 0, 0)).
                        build();
    }

    public static Message createMessage() {
        return createMessage(null);
    }

    public static MessageToParse.MessageToParseBuilder createMessageToParse(String source) {
        return MessageToParse.builder()
                .originalSource(source)
                .topic("test")
                .offset(0)
                .partition(0);
    }

    public static SignedSourceKey createOriginal(String topic) {
        return SignedSourceKey.builder().topic(topic).offset(0).partition(0).signature(new byte[128]).build();
    }
    public static SignedSourceKey createOriginal() {
        return createOriginal("test");
    }
}
