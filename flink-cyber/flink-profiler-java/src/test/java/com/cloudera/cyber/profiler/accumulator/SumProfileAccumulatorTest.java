package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.TestUtils;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.text.DecimalFormat;
import java.util.Collections;

public class SumProfileAccumulatorTest {
    private static final String SUM_RESULT_NAME = "sum_result";
    private static final String SUM_FIELD_NAME = "field_name";
    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("0.000000");

    @Test
    public void testSumWithValue() {
        SumProfileAccumulator acc = new SumProfileAccumulator(SUM_RESULT_NAME, SUM_FIELD_NAME, DEFAULT_FORMAT);

        Assert.assertEquals("0.000000", acc.getResult());
        testAddMessage(acc, "10", "10.000000");
        testAddMessage(acc, "5.5", "15.500000");
    }

    @Test
    public void testSumWithoutValue() {
        SumProfileAccumulator acc = new SumProfileAccumulator( SUM_FIELD_NAME, SUM_RESULT_NAME, DEFAULT_FORMAT);
        acc.add(TestUtils.createMessage(ImmutableMap.of(SUM_FIELD_NAME, "not a number")));
        Assert.assertEquals("0.000000", acc.getResult());

        acc.add(TestUtils.createMessage(Collections.emptyMap()));
        Assert.assertEquals("0.000000", acc.getResult());
    }

    @Test
    public void testMerge() {
        SumProfileAccumulator acc1 = new SumProfileAccumulator(SUM_RESULT_NAME, SUM_FIELD_NAME, DEFAULT_FORMAT);
        testAddMessage(acc1, "8", "8.000000");

        SumProfileAccumulator acc2 = new SumProfileAccumulator(SUM_RESULT_NAME, SUM_FIELD_NAME, DEFAULT_FORMAT);
        testAddMessage(acc2, "10", "10.000000");

        acc1.merge(acc2);
        Assert.assertEquals("18.000000", acc1.getResult());
        Assert.assertEquals("10.000000", acc2.getResult());

        // merging with null or a different profile accumulator doesn't affect the value
        acc1.merge(null);
        Assert.assertEquals("18.000000", acc1.getResult());
        Assert.assertEquals("10.000000", acc2.getResult());

        acc1.merge(new CountProfileAccumulator("result", DEFAULT_FORMAT));
        Assert.assertEquals("18.000000", acc1.getResult());
        Assert.assertEquals("10.000000", acc2.getResult());
    }

    private void testAddMessage(SumProfileAccumulator acc, String sumFieldValue, String expectedSum) {
        acc.add(TestUtils.createMessage(ImmutableMap.of(SUM_FIELD_NAME, sumFieldValue)));
        Assert.assertEquals(expectedSum, acc.getResult());
    }

}
