/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.enrichment;

import java.nio.charset.StandardCharsets;

public class EnrichmentUtils {
    public static final byte[] CF_ID = "id".getBytes(StandardCharsets.UTF_8);
    public static final byte[] CF_ENTRIES = "entries".getBytes(StandardCharsets.UTF_8);
    public static final byte[] Q_KEY = "key".getBytes(StandardCharsets.UTF_8);
    public static final byte[] Q_TYPE = "type".getBytes(StandardCharsets.UTF_8);

    public static byte[] enrichmentKey(String key) {
        // TODO salt the key for better distribution
        return (key).getBytes(StandardCharsets.UTF_8);
    }

}
