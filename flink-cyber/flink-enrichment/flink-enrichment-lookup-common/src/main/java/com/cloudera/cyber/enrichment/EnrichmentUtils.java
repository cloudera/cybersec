package com.cloudera.cyber.enrichment;

import java.nio.charset.StandardCharsets;

public class EnrichmentUtils {
    public static final byte[] CF_ID = "id".getBytes(StandardCharsets.UTF_8);
    public static final byte[] CF_ENTRIES = "entries".getBytes(StandardCharsets.UTF_8);
    public static final byte[] Q_KEY = "key".getBytes(StandardCharsets.UTF_8);
    public static final byte[] Q_TYPE = "type".getBytes(StandardCharsets.UTF_8);

    public static byte[] enrichmentKey(String type, String key) {
        // TODO salt the key for better distribution
        return (type + ":" + key).getBytes(StandardCharsets.UTF_8);
    }

}
