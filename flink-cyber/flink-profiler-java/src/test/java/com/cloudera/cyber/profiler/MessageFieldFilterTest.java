package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.TestUtils;
import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageFieldFilterTest {
    private static final Map<String, String> extensions = ImmutableMap.of("field_name1", "field_value_1",
            "field_name2", "field_value_2",
            "field_name3", "field_value_3");
    private static final String UNDEFINED_FIELD_NAME = "undefined field";
    private static final List<String> fieldNames = new ArrayList<>(extensions.keySet());

    @Test
    public void testFieldFilter() {
        verifyFieldFilter(fieldNames, true);
        verifyFieldFilter(Collections.singletonList(UNDEFINED_FIELD_NAME), false);
        List<String> allFieldsPlusUndefined = new ArrayList<>(fieldNames);
        allFieldsPlusUndefined.add(UNDEFINED_FIELD_NAME);
        verifyFieldFilter(allFieldsPlusUndefined, false);
    }

    @Test
    public void testNullFieldFilter() {
        try {
            new MessageFieldFilter(null);
            Assert.fail("Expected exception");
        } catch(NullPointerException npe) {
            Assert.assertEquals(MessageFieldFilter.NULL_MESSAGE_FIELD_LIST_ERROR, npe.getMessage());
        } catch (Exception e) {
            Assert.fail("wrong exception thrown");
        }
    }

    @Test
    public void testEmptyFieldFilter() {
        try {
            new MessageFieldFilter(Collections.emptyList());
            Assert.fail("Expected exception");
        } catch(IllegalArgumentException e) {
            Assert.assertEquals(MessageFieldFilter.EMPTY_MESSAGE_FIELD_LIST_ERROR, e.getMessage());
        } catch (Exception e) {
            Assert.fail("wrong exception thrown");
        }
    }

    private void verifyFieldFilter(List<String> filterFields, boolean expectedFilterValue) {
        Message message = Message.builder()
                .extensions(extensions)
                .ts(MessageUtils.getCurrentTimestamp())
                .originalSource(TestUtils.source("test", 0, 0))
                .source("test")
                .build();

        MessageFieldFilter messageFieldFilter = new MessageFieldFilter(filterFields);
        Assert.assertEquals(expectedFilterValue, messageFieldFilter.filter(message));
    }
}
