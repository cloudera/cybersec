package com.cloudera.cyber.dedupe.impl;

import com.cloudera.cyber.DedupeMessage;
import lombok.extern.java.Log;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.joda.time.Instant;

import java.util.UUID;

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
        DedupeMessage result = DedupeMessage.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setFields(sumAndMax.getFields())
                .setTs(sumAndMax.getMaxTs())
                .setStartTs(sumAndMax.getMinTs())
                .setCount(sumAndMax.getSum())
                .build();
        return result;
    }

    @Override
    public SumAndMax merge(SumAndMax acc, SumAndMax acc1) {
        return acc.merge(acc1);
    }
}
