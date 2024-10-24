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

package com.cloudera.cyber.dedupe.impl;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

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
