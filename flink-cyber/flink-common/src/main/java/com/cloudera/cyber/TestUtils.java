package com.cloudera.cyber;

import java.util.HashMap;
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

    public static Message createMessage() {
        return Message.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setTs(MessageUtils.getCurrentTimestamp())
                .setSource("test")
                .setOriginalSource(SignedSourceKey.newBuilder()
                        .setTopic("topic")
                        .setPartition(0)
                        .setOffset(0)
                        .setSignature(new sha1(new byte[128]))
                        .build())
                .setExtensions(new HashMap<>()).build();
    }
}
