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
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;


/**
 * Add the results of enrichments to a map
 *
 * The will merge two or more maps together. It will not overwrite values from the later maps
 */
public class MapMergeFunction extends ScalarFunction {
    public MapMergeFunction() {
        super();
    }

    public Map<String, String> eval(Map<String, String>... merge) {
        return Stream.of(merge)
                .map(s -> s.entrySet().stream())
                .flatMap(s -> s)
                .collect(toMap(
                        k -> k.getKey(),
                        v -> v.getValue(),
                        (o, n) -> o
                ));
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }
}
