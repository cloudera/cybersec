/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

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

    public static final Map<ProfileAggregationMethod, DecimalFormat> defaultFormat =
          ImmutableMap.<ProfileAggregationMethod, DecimalFormat>builder()
                      .put(COUNT, new DecimalFormat("#"))
                      .put(SUM, new DecimalFormat("#.##"))
                      .put(MIN, new DecimalFormat("#.##"))
                      .put(MAX, new DecimalFormat("#.##"))
                      .put(COUNT_DISTINCT, new DecimalFormat("#"))
                      .put(FIRST_SEEN, new DecimalFormat("#"))
                      .build();

    public static final Map<ProfileAggregationMethod, Boolean> usesFieldValue =
          ImmutableMap.<ProfileAggregationMethod, Boolean>builder()
                      .put(COUNT, false)
                      .put(SUM, true)
                      .put(MIN, true)
                      .put(MAX, true)
                      .put(COUNT_DISTINCT, true)
                      .put(FIRST_SEEN, false)
                      .build();
}
