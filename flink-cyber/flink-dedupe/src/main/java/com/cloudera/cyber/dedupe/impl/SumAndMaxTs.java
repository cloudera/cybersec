package com.cloudera.cyber.dedupe.impl;

import com.cloudera.cyber.dedupe.DedupeMessage;
import jdk.nashorn.internal.runtime.logging.Logger;
import org.apache.flink.api.common.functions.AggregateFunction;

import java.util.UUID;
import lombok.extern.java.Log;

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
                .id(UUID.randomUUID())
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
