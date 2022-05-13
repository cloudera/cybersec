package com.cloudera.cyber.generator;

import com.cloudera.cyber.generator.RandomGenerators;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SyntheticThreatEntry {
    private long ts;
    private RandomGenerators utils;
    private String ip;
}
