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

package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Slf4j
public class MapRowToEnrichmentCommand implements MapFunction<List<String>, EnrichmentCommand> {
    private static final String NULL_STRING_VALUE = "null";
    private final String enrichmentType;
    private final CommandType commandType;
    private final List<Tuple2<Integer, String>> fieldNames;
    private final List<Integer> keyFieldIndices;
    private final String keyDelimiter;

    @Override
    public EnrichmentCommand map(List<String> fields) {

        String enrichmentKey = keyFieldIndices.stream().map(keyFieldIndex -> Objects.toString(fields.get(keyFieldIndex), NULL_STRING_VALUE)).collect(Collectors.joining(keyDelimiter));

        Map<String, String> enrichmentValues = new HashMap<>();
        fieldNames.stream().
                forEach(fieldName -> enrichmentValues.put(fieldName.f1, fields.get(fieldName.f0)));

        EnrichmentEntry enrichmentEntry = EnrichmentEntry.builder().ts(MessageUtils.getCurrentTimestamp()).
                type(enrichmentType).
                key(enrichmentKey).entries(enrichmentValues).build();

        log.info("Writing enrichment key {}", enrichmentKey);

        return EnrichmentCommand.builder().
                headers(Collections.emptyMap()).
                type(commandType).payload(enrichmentEntry).build();
    }
}
