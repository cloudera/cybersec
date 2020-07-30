package com.cloudera.cyber.flink;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDUtils {
    public static byte[] asBytes(UUID uuid) {
        ByteBuffer b = ByteBuffer.wrap(new byte[16]);
        b.putLong(uuid.getMostSignificantBits());
        b.putLong(uuid.getLeastSignificantBits());
        return b.array();
    }
}
