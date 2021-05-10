package com.cloudera.cyber.profiler;


import com.cloudera.cyber.MessageUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageKeySelectorTest {
    private static final Map<String, String> extensions = ImmutableMap.of("field_name1", "field_value_1",
                                                                              "field_name2", "field_value_2",
                                                                              "field_name3", "field_value_3");

    private static final List<String> fieldNames = new ArrayList<>(extensions.keySet());

    @Test
    public void testSingleValueKey() {
        testKeyFields(1);
    }

    @Test
    public void testMultiValueKey() {
        testKeyFields(2);
        testKeyFields(3);
    }

    private void testKeyFields(int numKeyFields) {
        List<String>  keyFields = fieldNames.subList(0, numKeyFields);
        List<String> keyFieldValues = keyFields.stream().map(extensions::get).collect(Collectors.toList());
        String expectedKey = Joiner.on("-").join(keyFieldValues);

        ProfileMessage message = new ProfileMessage(MessageUtils.getCurrentTimestamp(), extensions);
        MessageKeySelector selector = new MessageKeySelector(keyFields);
        Assert.assertEquals(expectedKey, selector.getKey(message));
    }
}
