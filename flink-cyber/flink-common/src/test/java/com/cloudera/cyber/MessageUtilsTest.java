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

package com.cloudera.cyber;

import static java.util.stream.Collectors.toList;

import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MessageUtilsTest {

    private static final String TEST_FEATURE = "feature";
    private static final String TEST_FIELD_1 = "test 1";
    private static final String TEST_VALUE_1 = "value 1";
    private static final String TEST_FIELD_2 = "test 2";
    private static final String TEST_VALUE_2 = "value 2";

    private static final String TEST_MESSAGE_1 = "test message 1";
    private static final String TEST_MESSAGE_2 = "test message 2";

    private final HashMap<String, String> FIRST_FIELD_MAP = new HashMap<>();
    private final HashMap<String, String> SECOND_FIELD_MAP = new HashMap<>();
    private final HashMap<String, String> ALL_FIELDS_MAP = new HashMap<>();

    private final List<DataQualityMessage> DATA_QUALITY_MESSAGES_1 = Collections.singletonList(
          new DataQualityMessage(DataQualityMessageLevel.INFO.name(), TEST_FEATURE, TEST_FIELD_1, TEST_MESSAGE_1));
    private final List<DataQualityMessage> DATA_QUALITY_MESSAGES_1_DEEP_COPY =
          DATA_QUALITY_MESSAGES_1.stream().map(m -> m.toBuilder().build()).collect(toList());
    private final List<DataQualityMessage> DATA_QUALITY_MESSAGES_2 = Collections.singletonList(
          new DataQualityMessage(DataQualityMessageLevel.ERROR.name(), TEST_FEATURE, TEST_FIELD_2, TEST_MESSAGE_2));
    private final List<DataQualityMessage> ALL_DATA_QUALITY_MESSAGES =
          Stream.concat(DATA_QUALITY_MESSAGES_1.stream(), DATA_QUALITY_MESSAGES_2.stream()).collect(toList());

    @Before
    public void initTestExtensionMaps() {
        FIRST_FIELD_MAP.put(TEST_FIELD_1, TEST_VALUE_1);
        SECOND_FIELD_MAP.put(TEST_FIELD_2, TEST_VALUE_2);
        ALL_FIELDS_MAP.putAll(FIRST_FIELD_MAP);
        ALL_FIELDS_MAP.putAll(SECOND_FIELD_MAP);
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

    @Test
    public void testReplaceExtensions() {
        String extensionStaysSame = "unchanged_field";
        String extensionStaysSameValue = "original_value";
        String extensionToChange = "change_me";
        String extensionsToChangeOriginalValue = "old_value";
        String extensionsToChangeNewValue = "new_value";
        Message input = TestUtils.createMessage(ImmutableMap.of(extensionStaysSame, extensionStaysSameValue,
              extensionToChange, extensionsToChangeOriginalValue));
        Message output =
              MessageUtils.replaceFields(input, ImmutableMap.of(extensionToChange, extensionsToChangeNewValue));
        Assert.assertEquals(ImmutableMap.of(extensionStaysSame, extensionStaysSameValue,
              extensionToChange, extensionsToChangeNewValue), output.getExtensions());
    }

    @Test
    public void testReplaceExtensionsNullMap() {
        testReplaceNoOriginalEventExtensions(null);
    }

    @Test
    public void testReplaceExtensionsEmptyMap() {
        testReplaceNoOriginalEventExtensions(Collections.emptyMap());
    }

    private void testReplaceNoOriginalEventExtensions(Map<String, String> originalEventExtensions) {
        String extensionToChange = "change_me";
        String extensionsToChangeNewValue = "new_value";
        Message input = TestUtils.createMessage(originalEventExtensions);
        Message output =
              MessageUtils.replaceFields(input, ImmutableMap.of(extensionToChange, extensionsToChangeNewValue));
        Assert.assertEquals(ImmutableMap.of(extensionToChange, extensionsToChangeNewValue), output.getExtensions());
    }

    @Test
    public void testReplaceExtensionsNullReplaceExtensions() {
        testNoReplaceValues(null);
    }

    @Test
    public void testReplaceExtensionsEmptyReplaceExtensions() {
        testNoReplaceValues(Collections.emptyMap());
    }

    void testNoReplaceValues(Map<String, String> noReplaceValues) {
        Map<String, String> originalExtensions = ImmutableMap.of("field_1", "value_1",
              "field_2", "value_2");
        Message input = TestUtils.createMessage(originalExtensions);
        Message output = MessageUtils.replaceFields(input, noReplaceValues);
        Assert.assertEquals(originalExtensions, output.getExtensions());

    }

}
