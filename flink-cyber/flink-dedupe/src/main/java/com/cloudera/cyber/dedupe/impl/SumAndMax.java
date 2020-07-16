package com.cloudera.cyber.dedupe.impl;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SumAndMax {
    private Long sum;
    private Long maxTs;
    private Long minTs;
    private Map<String, String> fields;

    public SumAndMax merge(SumAndMax other) {
        if (fields.equals(other.getFields())) {
            throw new IllegalStateException("Unmatched SumAndMax");
        }
        return SumAndMax.builder()
                .fields(this.getFields())
                .maxTs(Math.max(this.getMaxTs(), other.getMaxTs()))
                .minTs(Math.min(this.getMinTs(), other.getMinTs()))
                .sum(this.getSum() + other.getSum())
                .build();
    }
}
