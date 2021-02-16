package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.TestUtils;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class CountDistinctProfileAccumulatorTest {
    private static final String FIELD_NAME = "field_name";
    private static final String TEST_RESULT_NAME = "distinct_count";

    @Test
    public void testAdd() {
        String duplicateString = "duplicate";
        String uniqueString = "unique";
        CountDistinctProfileAccumulator accumulator = testCountDistinctString(Arrays.asList(duplicateString, uniqueString, duplicateString));

        // add a null value - should not change value
        String result = accumulator.getResult();
        accumulator.add(TestUtils.createMessage(Collections.emptyMap()));

        Assert.assertEquals(result, accumulator.getResult());
    }

    @Test
    public void testAddNullFieldValue() {
        String duplicateString = "duplicate";
        testCountDistinctString(Arrays.asList(duplicateString, null, duplicateString));
    }

    @Test
    public void testMerge() {

        String duplicateString1 = "duplicate_1";
        String duplicateString2 = "duplicate_2";
        String uniqueString = "unique";
        CountDistinctProfileAccumulator acc1 = testCountDistinctString(Arrays.asList(duplicateString1, duplicateString2));
        CountDistinctProfileAccumulator acc2 = testCountDistinctString(Arrays.asList(duplicateString1, duplicateString2, uniqueString));

        acc1.merge(acc2);

        Assert.assertEquals("3", acc1.getResult());
    }

    @Test
    public void testMergeEmptyThis() {

        CountDistinctProfileAccumulator emptyAcc = testCountDistinctString(Collections.emptyList());
        CountDistinctProfileAccumulator nonEmptyAcc = testCountDistinctString(Arrays.asList("First", "Second"));
        String nonEmptyAccResult = nonEmptyAcc.getResult();

        emptyAcc.merge(nonEmptyAcc);
        Assert.assertEquals(nonEmptyAccResult, emptyAcc.getResult());
    }

    @Test
    public void testMergeNullOther() {

        CountDistinctProfileAccumulator emptyAcc = testCountDistinctString(Collections.emptyList());

        emptyAcc.merge(null);
        Assert.assertEquals("0", emptyAcc.getResult());
    }

    @Test
    public void testMergeEmptyOther() {
        CountDistinctProfileAccumulator emptyAcc = testCountDistinctString(Collections.emptyList());
        CountDistinctProfileAccumulator nonEmptyAcc = testCountDistinctString(Arrays.asList("First", "Second"));
        String nonEmptyAccResult = nonEmptyAcc.getResult();

        nonEmptyAcc.merge(emptyAcc);
        Assert.assertEquals(nonEmptyAccResult, nonEmptyAcc.getResult());
    }

    @Test
    public void testMergeOtherWithWrongType() {
        CountProfileAccumulator wrongTypeAcc = new CountProfileAccumulator("wrong_type", null);
        wrongTypeAcc.add(TestUtils.createMessage(ImmutableMap.of(FIELD_NAME, "counted_value")));

        CountDistinctProfileAccumulator nonEmptyAcc = testCountDistinctString(Arrays.asList("First", "Second"));
        String nonEmptyAccResult = nonEmptyAcc.getResult();

        nonEmptyAcc.merge(wrongTypeAcc);
        Assert.assertEquals(nonEmptyAccResult, nonEmptyAcc.getResult());
    }

    @Test
    public void testBothEmpty() {
        CountDistinctProfileAccumulator empty1 = testCountDistinctString(Collections.emptyList());
        CountDistinctProfileAccumulator  empty2= testCountDistinctString(Collections.emptyList());

        empty1.merge(empty2);
        Assert.assertEquals("0", empty1.getResult());
    }

    private CountDistinctProfileAccumulator testCountDistinctString(List<String> strings) {
        CountDistinctProfileAccumulator   acc = new CountDistinctProfileAccumulator(TEST_RESULT_NAME, FIELD_NAME, null);
        Map<String, String> extensions = new HashMap<>();
        for(String nextString : strings) {
            extensions.put(FIELD_NAME, nextString);
            acc.add(TestUtils.createMessage(extensions));
        }
        String uniqueStringCount = Long.toString(strings.stream().filter(Objects::nonNull).distinct().count());
        Assert.assertEquals(uniqueStringCount, acc.getResult());
        return acc;
    }
}
