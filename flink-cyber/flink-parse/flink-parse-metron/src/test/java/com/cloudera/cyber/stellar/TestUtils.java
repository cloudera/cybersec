package com.cloudera.cyber.stellar;

import java.nio.charset.StandardCharsets;

public class TestUtils {
    public static byte[] toBytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
