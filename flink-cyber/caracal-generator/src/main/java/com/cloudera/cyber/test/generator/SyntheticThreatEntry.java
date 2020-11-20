package com.cloudera.cyber.test.generator;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyntheticThreatEntry {
    private long ts;
    private RandomGenerators utils;
    private String ip;
}
