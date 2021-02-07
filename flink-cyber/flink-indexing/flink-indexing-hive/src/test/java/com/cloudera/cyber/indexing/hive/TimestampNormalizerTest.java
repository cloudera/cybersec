package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.MessageUtils;
import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

public class TimestampNormalizerTest {

    private final SimpleDateFormat hiveDateFormatter = new SimpleDateFormat(HiveStreamingMessageWriter.HIVE_DATE_FORMAT);
    private final TimestampNormalizer normalizer = new TimestampNormalizer(HiveStreamingMessageWriter.DEFAULT_TIMESTAMP_FORMATS,
            hiveDateFormatter);

    @Test
    public void testLongTimestamp() {
        long currentMillis = MessageUtils.getCurrentTimestamp();
        String expectedTimestamp = hiveDateFormatter.format(Date.from(Instant.ofEpochMilli(currentMillis)));
        String fieldValue = Long.toString(currentMillis);
        Assert.assertEquals(expectedTimestamp, normalizer.apply(fieldValue));
    }

    @Test
    public void testFirstFormattedTimestamp() {
        String fieldValue = "2020-11-19 22:00:01.000000";
        Assert.assertEquals("2020-11-19 22:00:01.000", normalizer.apply(fieldValue));
    }

    @Test
    public void testLastFormattedTimestamp() {
        String fieldValue = "2020-01-15T23:05:33Z";
        Assert.assertEquals("2020-01-15 23:05:33.000", normalizer.apply(fieldValue));
    }

    @Test(expected=IllegalStateException.class)
    public void testTimestampWithoutMatch() {
        String fieldValue = "not a timestamp";
        normalizer.apply(fieldValue);
    }
}
