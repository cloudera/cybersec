package com.cloudera.cyber.profiler;


import com.google.common.collect.ImmutableMap;

import java.text.DecimalFormat;
import java.util.Map;

public enum ProfileAggregationMethod {
    COUNT,
    SUM,
    MIN,
    MAX,
    COUNT_DISTINCT,
    FIRST_SEEN;

    public static final Map<ProfileAggregationMethod, DecimalFormat> defaultFormat = ImmutableMap.<ProfileAggregationMethod, DecimalFormat>builder().
            put(COUNT, new DecimalFormat("#")).
            put(SUM, new DecimalFormat("#.##")).
            put(MIN, new DecimalFormat("#.##")).
            put(MAX, new DecimalFormat("#.##")).
            put(COUNT_DISTINCT, new DecimalFormat("#")).
            put(FIRST_SEEN, new DecimalFormat("#")).
            build();

    public static final Map<ProfileAggregationMethod, Boolean> usesFieldValue = ImmutableMap.<ProfileAggregationMethod, Boolean>builder().
            put(COUNT, false).
            put(SUM, true).
            put(MIN, true).
            put(MAX, true).
            put(COUNT_DISTINCT, true).
            put(FIRST_SEEN, false).
            build();
}
