package com.cloudera.cyber.test.generator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyntheticEntry {
    private long ts;

    private RandomGenerators utils;
}
