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

package com.cloudera.cyber.enrichment.hbase;

import org.apache.flink.table.functions.ScalarFunction;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

public class PrefixMapFunction extends ScalarFunction {
    public Map<String, String> eval(Map<String, String> map, String prefix) {
        return map.entrySet().stream()
                .collect(toMap(
                        k -> prefix + k.getKey(),
                        v -> v.getValue())
                );
    }
}
