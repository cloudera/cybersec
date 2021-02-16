package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.TestUtils;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.Collections;

public class MaximumProfileAccumulatorTest {
    private static final String RESULT_NAME = "result";
    private static final String FIELD_NAME = "field_name";
    private static final String DOUBLE_MIN_STRING = String.format("%f", Double.MIN_VALUE);
    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("0.000000");

    @Test
    public void testMaxWithValue() {
        MaximumProfileAccumulator acc = new MaximumProfileAccumulator(RESULT_NAME, FIELD_NAME, DEFAULT_FORMAT);

        Assert.assertEquals(DOUBLE_MIN_STRING, acc.getResult());
        testAddMessage(acc, "10", "10.000000");
        testAddMessage(acc, "5.5", "10.000000");
    }

    @Test
    public void testMaxWithoutValue() {
        MaximumProfileAccumulator acc = new MaximumProfileAccumulator( RESULT_NAME, FIELD_NAME, DEFAULT_FORMAT);
        acc.add(TestUtils.createMessage(ImmutableMap.of(FIELD_NAME, "not a number")));
        Assert.assertEquals(DOUBLE_MIN_STRING, acc.getResult());

        acc.add(TestUtils.createMessage(Collections.emptyMap()));
        Assert.assertEquals(DOUBLE_MIN_STRING, acc.getResult());
    }

    @Test
    public void testMerge() {
        MaximumProfileAccumulator acc1 = new MaximumProfileAccumulator(RESULT_NAME, FIELD_NAME, DEFAULT_FORMAT);
        testAddMessage(acc1, "10", "10.000000");

        MaximumProfileAccumulator acc2 = new MaximumProfileAccumulator(RESULT_NAME, FIELD_NAME, DEFAULT_FORMAT);
        testAddMessage(acc2, "50.000000", "50.000000");

        acc1.merge(acc2);
        Assert.assertEquals("50.000000", acc1.getResult());
        Assert.assertEquals("50.000000", acc2.getResult());

        // merging with null or a different profile accumulator doesn't affect the value
        acc1.merge(null);
        Assert.assertEquals("50.000000", acc1.getResult());
        Assert.assertEquals("50.000000", acc2.getResult());

        acc1.merge(new CountProfileAccumulator("result", DEFAULT_FORMAT));
        Assert.assertEquals("50.000000", acc1.getResult());
        Assert.assertEquals("50.000000", acc2.getResult());
    }

    private void testAddMessage(MaximumProfileAccumulator acc, String sumFieldValue, String expectedResult) {
        acc.add(TestUtils.createMessage(ImmutableMap.of(FIELD_NAME, sumFieldValue)));
        Assert.assertEquals(expectedResult, acc.getResult());
    }
}
