package com.cloudera.cyber.profiler;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public enum ProfileAggregationMethod {
    COUNT,
    SUM,
    MIN,
    MAX,
    COUNT_DISTINCT,
    FIRST_SEEN;

    public static final Map<ProfileAggregationMethod, DecimalFormat> defaultFormat = new HashMap<ProfileAggregationMethod, DecimalFormat>() {{
        put(COUNT, new DecimalFormat("#"));
        put(SUM, new DecimalFormat("#.##"));
        put(MIN,  new DecimalFormat("#.##"));
        put(MAX,  new DecimalFormat("#.##"));
        put(COUNT_DISTINCT, new DecimalFormat("#"));
        put(FIRST_SEEN, new DecimalFormat("#"));
    }};
}
