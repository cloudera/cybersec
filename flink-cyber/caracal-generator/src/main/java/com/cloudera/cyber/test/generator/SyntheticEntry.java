package com.cloudera.cyber.test.generator;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class SyntheticEntry {
    private long ts;
    private RandomGenerators utils;
}
