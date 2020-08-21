package com.cloudera.cyber;

import avro.shaded.com.google.common.collect.Sets;
import com.cloudera.cyber.data.quality.DataQualityMessage;
import com.cloudera.cyber.data.quality.MessageLevel;
import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageUtilsTest {

    private static final String TEST_FEATURE = "feature";
    private static final String TEST_FIELD = "test";
    private static final String TEST_VALUE = "value";
    private static final String TEST_ENRICHMENT = "enrichment";

    @Test
    public void testAddFields() {
        Map<String, Object> fields = new HashMap<>();
        fields.put(TEST_FIELD, TEST_VALUE);

        Message input = TestUtils.createMessage();

        Message output = MessageUtils.addFields(input, fields);

        Assert.assertEquals(fields, output.getExtensions());
    }

    @Test
    public void testReportDataQualityMessage() {
        Message input = TestUtils.createMessage();

        String messageText = "This is an info message.";
        MessageLevel level = MessageLevel.ERROR;
        Message output = MessageUtils.reportQualityMessage(input, level, TEST_FIELD, TEST_FEATURE, messageText);

        List<DataQualityMessage> dataQualityMessages = output.getDataQualityMessages();
        Assert.assertEquals(1, dataQualityMessages.size());

        DataQualityMessage message = dataQualityMessages.get(0);
        Assert.assertEquals(level, message.getLevel());
        Assert.assertEquals(TEST_FIELD, message.getField());
        Assert.assertEquals(TEST_FEATURE, message.getFeature());
        Assert.assertEquals(messageText, message.getMessageText());
    }

    @Test
    public void testEnrich() {
        Message input = TestUtils.createMessage();
        Message output = MessageUtils.enrich(input, TEST_FIELD, TEST_FEATURE, TEST_ENRICHMENT, TEST_VALUE);
        Map<String, Object> extensions = output.getExtensions();
        Assert.assertEquals(1, extensions.size());
        Assert.assertEquals(TEST_VALUE, extensions.get(String.join(".", TEST_FIELD, TEST_FEATURE, TEST_ENRICHMENT)));
    }

    @Test
    public void testEnrichNullValue() {
        Message input = TestUtils.createMessage();
        Message output = MessageUtils.enrich(input, TEST_FIELD, TEST_FEATURE, TEST_ENRICHMENT, null);
        Assert.assertTrue(output.getExtensions().isEmpty());
    }

    @Test
    public void testEnrichSetValue() {
        Message input = TestUtils.createMessage();
        String[] testEnrichmentValues = new String[] {"first", "second", "first"};
        Message output = null;
        for(String enrichmentValue : testEnrichmentValues) {
            output = MessageUtils.enrichSet(input, TEST_FIELD, TEST_FEATURE, TEST_ENRICHMENT, enrichmentValue);
            Assert.assertNotNull(output );
        }
        Assert.assertEquals(Sets.newHashSet(testEnrichmentValues), output.getExtensions().get(String.join(".", TEST_FIELD, TEST_FEATURE, TEST_ENRICHMENT)));
    }

    @Test
    public void testEnrichSetNull() {
        Message input = TestUtils.createMessage();
        Message output = MessageUtils.enrichSet(input, TEST_FIELD, TEST_FEATURE, TEST_ENRICHMENT, null);
        Assert.assertTrue(output.getExtensions().isEmpty());
    }

    @Test
    public void testGetCurrentTime() {
        long currentTime = MessageUtils.getCurrentTimestamp();
        long currentMillis = Instant.now().toEpochMilli();

        Assert.assertTrue(currentMillis - currentTime < 1000);
    }
}
