package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.TestUtils;
import com.fasterxml.jackson.jaxrs.json.annotation.JSONP;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.Collections;

public class MinimumProfileAccumulatorTest {
    private static final String RESULT_NAME = "result";
    private static final String FIELD_NAME = "field_name";
    private static final String DOUBLE_MAX_STRING = String.format("%f", Double.MAX_VALUE);
    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("#.000000");

    @Test
    public void testMinWithValue() {
        MinimumProfileAccumulator acc = new MinimumProfileAccumulator(RESULT_NAME, FIELD_NAME, DEFAULT_FORMAT);

        Assert.assertEquals(String.format("%f", Double.MAX_VALUE), acc.getResult());
        testAddMessage(acc, "10", "10.000000");
        testAddMessage(acc, "5.5", "5.500000");
    }

    @Test
    public void testMinWithoutValue() {
        MinimumProfileAccumulator acc = new MinimumProfileAccumulator( RESULT_NAME, FIELD_NAME, DEFAULT_FORMAT);
        acc.add(TestUtils.createMessage(ImmutableMap.of(FIELD_NAME, "not a number")));
        Assert.assertEquals(DOUBLE_MAX_STRING, acc.getResult());

        acc.add(TestUtils.createMessage(Collections.emptyMap()));
        Assert.assertEquals(DOUBLE_MAX_STRING, acc.getResult());
    }

    @Test
    public void testMerge() {
        MinimumProfileAccumulator acc1 = new MinimumProfileAccumulator(RESULT_NAME, FIELD_NAME, DEFAULT_FORMAT);
        testAddMessage(acc1, "10", "10.000000");

        MinimumProfileAccumulator acc2 = new MinimumProfileAccumulator(RESULT_NAME, FIELD_NAME, DEFAULT_FORMAT);
        testAddMessage(acc2, "8", "8.000000");

        acc1.merge(acc2);
        Assert.assertEquals("8.000000", acc1.getResult());
        Assert.assertEquals("8.000000", acc2.getResult());

        // merging with null or a different profile accumulator doesn't affect the value
        acc1.merge(null);
        Assert.assertEquals("8.000000", acc1.getResult());
        Assert.assertEquals("8.000000", acc2.getResult());

        acc1.merge(new CountProfileAccumulator("result", null));
        Assert.assertEquals("8.000000", acc1.getResult());
        Assert.assertEquals("8.000000", acc2.getResult());
    }

    private void testAddMessage(MinimumProfileAccumulator acc, String sumFieldValue, String expectedResult) {
        acc.add(TestUtils.createMessage(ImmutableMap.of(FIELD_NAME, sumFieldValue)));
        Assert.assertEquals(expectedResult, acc.getResult());
    }}
