package com.cloudera.cyber.test.generator;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class SyntheticEntry {
    private long ts;

    private RandomGenerators utils;
}
