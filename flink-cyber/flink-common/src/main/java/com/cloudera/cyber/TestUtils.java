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
        return createMessage(MessageUtils.getCurrentTimestamp(), "test", extensions);
    }

    public static Message createMessage(long timestamp, String source, Map<String, String> extensions) {
        return Message.builder()
                .ts(timestamp)
                .source(source)
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
        return createMessageToParse(source, "test");
    }

    public static MessageToParse.MessageToParseBuilder createMessageToParse(String source, String topic) {
        return MessageToParse.builder()
                .originalSource(source)
                .topic(topic)
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
