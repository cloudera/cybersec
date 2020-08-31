package com.cloudera.cyber;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class MessageUtilsTest {

    private static final String TEST_FEATURE = "feature";
    private static final String TEST_FIELD_1 = "test 1";
    private static final String TEST_VALUE_1 = "value 1";
    private static final String TEST_FIELD_2 = "test 2";
    private static final String TEST_VALUE_2 = "value 2";
    private static final List<String> TEST_ARRAY_1 = Arrays.asList( "A", "B", "C");
    private static final List<Integer> TEST_ARRAY_2 = Arrays.asList( 1, 2, 3);

    private static final String TEST_MESSAGE_1 = "test message 1";
    private static final String TEST_MESSAGE_2 = "test message 2";

    private final HashMap<String, Object> FIRST_FIELD_MAP = new HashMap<>();
    private final HashMap<String, Object> SECOND_FIELD_MAP = new HashMap<>();
    private final HashMap<String, Object> ALL_FIELDS_MAP = new HashMap<>();
    private final HashMap<String, Object> STRING_ARRAY_FIELD_MAP = new HashMap<>();
    private final HashMap<String, Object> INT_ARRAY_FIELD_MAP = new HashMap<>();
    private final HashMap<String, Object> ALL_ARRAY_FIELDS_MAP = new HashMap<>();

    private final List<DataQualityMessage> DATA_QUALITY_MESSAGES_1 = Collections.singletonList(new DataQualityMessage(DataQualityMessageLevel.INFO, TEST_FEATURE, TEST_FIELD_1, TEST_MESSAGE_1));
    private final List<DataQualityMessage> DATA_QUALITY_MESSAGES_1_DEEP_COPY = DATA_QUALITY_MESSAGES_1.stream().map(m -> DataQualityMessage.newBuilder(m).build()).collect(toList());
    private final List<DataQualityMessage> DATA_QUALITY_MESSAGES_2 = Collections.singletonList(new DataQualityMessage(DataQualityMessageLevel.ERROR, TEST_FEATURE, TEST_FIELD_2, TEST_MESSAGE_2));
    private final List<DataQualityMessage> ALL_DATA_QUALITY_MESSAGES = Stream.concat(DATA_QUALITY_MESSAGES_1.stream(), DATA_QUALITY_MESSAGES_2.stream()).collect(toList());

    @Before
    public void initTestExtensionMaps() {
        FIRST_FIELD_MAP.put(TEST_FIELD_1, TEST_VALUE_1);
        SECOND_FIELD_MAP.put(TEST_FIELD_2, TEST_VALUE_2);
        ALL_FIELDS_MAP.putAll(FIRST_FIELD_MAP);
        ALL_FIELDS_MAP.putAll(SECOND_FIELD_MAP);
        STRING_ARRAY_FIELD_MAP.put(TEST_FIELD_1, TEST_ARRAY_1);
        INT_ARRAY_FIELD_MAP.put(TEST_FIELD_2, TEST_ARRAY_2);
        ALL_ARRAY_FIELDS_MAP.putAll(STRING_ARRAY_FIELD_MAP);
        ALL_ARRAY_FIELDS_MAP.putAll(INT_ARRAY_FIELD_MAP);
    }

    @Test
    public void testAddFields() {
        Message input = TestUtils.createMessage();

        Message output1 = MessageUtils.addFields(input, FIRST_FIELD_MAP);
        Assert.assertEquals(FIRST_FIELD_MAP, output1.getExtensions());

        Message output2 = MessageUtils.addFields(output1, SECOND_FIELD_MAP);
        Assert.assertNotSame(output1, output2);
        Assert.assertEquals(ALL_FIELDS_MAP, output2.getExtensions());
    }

    @Test
    public void testAddEmptyFields() {
        Message input = TestUtils.createMessage();

        Message output = MessageUtils.addFields(input, Collections.emptyMap());
        Assert.assertSame(input, output);
    }

    @Test
    public void testAddArrayFields() {
        Message input = TestUtils.createMessage();

        Message output1 = MessageUtils.addFields(input, STRING_ARRAY_FIELD_MAP);
        Assert.assertEquals(STRING_ARRAY_FIELD_MAP, output1.getExtensions());

        Message output2 = MessageUtils.addFields(output1, INT_ARRAY_FIELD_MAP);
        Assert.assertEquals(ALL_ARRAY_FIELDS_MAP, output2.getExtensions());
    }

   @Test
    public void testEnrichExtensions() {
        Message input = TestUtils.createMessage();

        List<DataQualityMessage> expectedDataQualityMessages = new ArrayList<>();
        Message output1 = MessageUtils.enrich(input, FIRST_FIELD_MAP, expectedDataQualityMessages);
       Assert.assertNotSame(input, output1);
        Assert.assertEquals(FIRST_FIELD_MAP, output1.getExtensions());
        Assert.assertEquals(Collections.emptyList(), output1.getDataQualityMessages());

        Message output2 = MessageUtils.enrich(output1, SECOND_FIELD_MAP, expectedDataQualityMessages);
        Assert.assertEquals(ALL_FIELDS_MAP, output2.getExtensions());
        Assert.assertEquals(Collections.emptyList(), output1.getDataQualityMessages());
    }

    @Test
    public void testEnrichExtensionsNoChanges() {
        Message input = TestUtils.createMessage();
        Message output = MessageUtils.enrich(input, new HashMap<>(), new ArrayList<>());
        Assert.assertSame(input, output);
    }

    @Test
    public void testEnrichDataQualityMessages() {
        Message input = TestUtils.createMessage();
        Message output1 = MessageUtils.enrich(input, Collections.emptyMap(), DATA_QUALITY_MESSAGES_1);
        Assert.assertTrue(output1.getExtensions().isEmpty());
        Assert.assertEquals(DATA_QUALITY_MESSAGES_1, output1.getDataQualityMessages());

        Message output2 = MessageUtils.enrich(output1, Collections.emptyMap(), DATA_QUALITY_MESSAGES_1_DEEP_COPY);
        Assert.assertEquals(DATA_QUALITY_MESSAGES_1, output2.getDataQualityMessages());

        Message output3 = MessageUtils.enrich(output2, Collections.emptyMap(), DATA_QUALITY_MESSAGES_2);
        Assert.assertEquals(ALL_DATA_QUALITY_MESSAGES, output3.getDataQualityMessages());
    }

    @Test
    public void testGetCurrentTime() {
        long currentTime = MessageUtils.getCurrentTimestamp();
        long currentMillis = Instant.now().toEpochMilli();

        Assert.assertTrue(currentMillis - currentTime < 1000);
    }
}
