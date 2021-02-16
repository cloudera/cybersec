package com.cloudera.cyber.profiler.accumulator;

import org.junit.Assert;
import org.junit.Test;

public class CountProfileAccumulatorTest {
    private static final String COUNT_RESULT_EXTENSION_NAME = "count_result";

    @Test
    public void testCountAccumulator() {
        CountProfileAccumulator acc1 = new CountProfileAccumulator(COUNT_RESULT_EXTENSION_NAME, null);
        Assert.assertEquals("0", acc1.getResult());
        Assert.assertEquals(COUNT_RESULT_EXTENSION_NAME, acc1.getResultExtensionName());
        // message content is not used for counting
        acc1.add(null);
        Assert.assertEquals("1", acc1.getResult());
        acc1.add(null);
        Assert.assertEquals("2", acc1.getResult());

        CountProfileAccumulator acc2 = new CountProfileAccumulator(COUNT_RESULT_EXTENSION_NAME, null);
        acc2.add(null);
        Assert.assertEquals("2", acc1.getResult());

        acc1.merge(acc2);
        Assert.assertEquals("3", acc1.getResult());

        acc1.merge(null);
        Assert.assertEquals("3", acc1.getResult());

        acc1.merge(new SumProfileAccumulator("result", "extname", null));
        Assert.assertEquals("3", acc1.getResult());
    }



}
