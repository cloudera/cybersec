package com.cloudera.cyber;

import java.util.Map;
import java.util.UUID;

public class TestUtils {

    public static SignedSourceKey source(String topic, int partition, long offset) {
        return SignedSourceKey.newBuilder()
                .setTopic(topic)
                .setPartition(partition)
                .setOffset(offset)
                .setSignature(new sha1(new byte[128]))
                .build();
    }

    public static Message createMessage(Map<String, Object> extensions) {
        return Message.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTs(MessageUtils.getCurrentTimestamp())
                .setSource("test")
                .setExtensions(extensions)
                .setMessage("")
                .setOriginalSource(
                        source("topic", 0, 0)).
                        build();
    }

    public static Message createMessage() {
        return createMessage(null);
    }
}
