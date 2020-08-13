package com.cloudera.cyber;

public class TestUtils {

    public static SignedSourceKey source(String topic, int partition, long offset) {
        return SignedSourceKey.newBuilder()
                .setTopic("test")
                .setPartition(0)
                .setOffset(0)
                .setSignature(new sha1(new byte[128]))
                .build();
    }
}
