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

package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.MessageUtils;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TimestampNormalizerTest {

    private final SimpleDateFormat hiveDateFormatter = new SimpleDateFormat(HiveStreamingMessageWriter.HIVE_DATE_FORMAT);
    private final TimestampNormalizer normalizer = new TimestampNormalizer(HiveStreamingMessageWriter.DEFAULT_TIMESTAMP_FORMATS,
            hiveDateFormatter);

    @Test
    public void testLongTimestamp() {
        long currentMillis = MessageUtils.getCurrentTimestamp();
        String expectedTimestamp = hiveDateFormatter.format(Date.from(Instant.ofEpochMilli(currentMillis)));
        String fieldValue = Long.toString(currentMillis);
        assertEquals(expectedTimestamp, normalizer.apply(fieldValue));
    }

    @Test
    public void testFirstFormattedTimestamp() {
        String fieldValue = "2020-11-19 22:00:01.000000";
        assertEquals("2020-11-19 22:00:01.000", normalizer.apply(fieldValue));
    }

    @Test
    public void testLastFormattedTimestamp() {
        String fieldValue = "2020-01-15T23:05:33Z";
        assertEquals("2020-01-15 23:05:33.000", normalizer.apply(fieldValue));
    }

    @Test
    public void testTimestampWithoutMatch() {
        String fieldValue = "not a timestamp";
        assertThrows(IllegalStateException.class, () -> normalizer.apply(fieldValue));
    }
}
