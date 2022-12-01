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

package com.cloudera.cyber.dedupe.impl;

import com.cloudera.cyber.DedupeMessage;
import lombok.extern.java.Log;
import org.apache.flink.api.common.functions.AggregateFunction;

@Log
public class SumAndMaxTs implements AggregateFunction<DedupeMessage, SumAndMax, DedupeMessage> {

    @Override
    public SumAndMax createAccumulator() {
        return SumAndMax.builder()
                .sum(0L)
                .maxTs(Long.MIN_VALUE)
                .minTs(Long.MAX_VALUE)
                .build();
    }

    @Override
    public SumAndMax add(DedupeMessage dedupeMessage, SumAndMax sumAndMax) {
        if (sumAndMax.getFields() != null && !sumAndMax.getFields().equals(dedupeMessage.getFields())) {
            throw new IllegalStateException("Unmatched key in accumulator");
        }
        return SumAndMax.builder()
                .fields(dedupeMessage.getFields())
                .maxTs(Math.max(dedupeMessage.getTs(), sumAndMax.getMaxTs()))
                .minTs(Math.min(dedupeMessage.getTs(), sumAndMax.getMinTs()))
                .sum(dedupeMessage.getCount() + sumAndMax.getSum())
                .build();
    }

    @Override
    public DedupeMessage getResult(SumAndMax sumAndMax) {
        DedupeMessage result = DedupeMessage.builder()
                .fields(sumAndMax.getFields())
                .ts(sumAndMax.getMaxTs())
                .startTs(sumAndMax.getMinTs())
                .count(sumAndMax.getSum())
                .build();
        return result;
    }

    @Override
    public SumAndMax merge(SumAndMax acc, SumAndMax acc1) {
        return acc.merge(acc1);
    }
}
