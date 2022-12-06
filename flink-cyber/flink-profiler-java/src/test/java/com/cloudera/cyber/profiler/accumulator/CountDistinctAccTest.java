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

package com.cloudera.cyber.profiler.accumulator;

import org.apache.flink.api.common.accumulators.Accumulator;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CountDistinctAccTest {

    @Test
    public void testAddFollowedByResetLocal() {
        String duplicateString = "duplicate";
        String uniqueString = "unique";
        CountDistinctAcc accumulator = testCountDistinctString(Arrays.asList(duplicateString, uniqueString, duplicateString));

        double previousResult = accumulator.getEstimate();

        // add a null value - should not change value
        accumulator.add(null);
        Assert.assertEquals(previousResult, accumulator.getEstimate(), 0.1);

        // clone should have same result as original
        Accumulator<String, SerializableUnion> copy = accumulator.clone();
        Assert.assertEquals(previousResult, copy.getLocalValue().getUnion().getResult().getEstimate(), 0.1);

        // reset local reset union
        accumulator.resetLocal();
        Assert.assertEquals(0, accumulator.getEstimate(),0.1);
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
        CountDistinctAcc acc1 = testCountDistinctString(Arrays.asList(duplicateString1, duplicateString2));
        CountDistinctAcc acc2 = testCountDistinctString(Arrays.asList(duplicateString1, duplicateString2, uniqueString));

        acc1.merge(acc2);

        Assert.assertEquals(3, acc1.getEstimate(), 0.1);
    }

    @Test
    public void testMergeEmptyThis() {

        CountDistinctAcc emptyAcc = testCountDistinctString(Collections.emptyList());
        CountDistinctAcc nonEmptyAcc = testCountDistinctString(Arrays.asList("First", "Second"));
        double nonEmptyAccResult = nonEmptyAcc.getEstimate();

        emptyAcc.merge(nonEmptyAcc);
        Assert.assertEquals(nonEmptyAccResult, emptyAcc.getEstimate(), 0.1);
    }

    @Test
    public void testMergeNullOther() {

        CountDistinctAcc emptyAcc = testCountDistinctString(Collections.emptyList());

        emptyAcc.merge(null);
        Assert.assertEquals(0, emptyAcc.getEstimate(), 0.1);
    }

    @Test
    public void testMergeEmptyOther() {
        CountDistinctAcc emptyAcc = testCountDistinctString(Collections.emptyList());
        CountDistinctAcc nonEmptyAcc = testCountDistinctString(Arrays.asList("First", "Second"));
        double nonEmptyAccResult = nonEmptyAcc.getEstimate();

        nonEmptyAcc.merge(emptyAcc);
        Assert.assertEquals(nonEmptyAccResult, nonEmptyAcc.getEstimate(), 0.1);
    }

    @Test
    public void testBothEmpty() {
        CountDistinctAcc empty1 = testCountDistinctString(Collections.emptyList());
        CountDistinctAcc empty2= testCountDistinctString(Collections.emptyList());

        empty1.merge(empty2);
        Assert.assertEquals(0, empty1.getEstimate(), 0.1);
    }

    private CountDistinctAcc testCountDistinctString(List<String> strings) {
        CountDistinctAcc   acc = new CountDistinctAcc();
        for(String nextString : strings) {
            acc.add(nextString);
        }
        long uniqueStringCount = strings.stream().filter(Objects::nonNull).distinct().count();
        Assert.assertEquals(uniqueStringCount, acc.getEstimate(), 0.1);
        return acc;
    }
}
