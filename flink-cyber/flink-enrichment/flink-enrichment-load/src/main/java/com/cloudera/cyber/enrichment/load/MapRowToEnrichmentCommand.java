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
import org.apache.flink.types.Row;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@AllArgsConstructor
@Slf4j
public class MapRowToEnrichmentCommand implements MapFunction<Row, EnrichmentCommand> {
    private static final String NULL_STRING_VALUE = "null";
    private final String enrichmentType;
    private final CommandType commandType;
    private final List<String> fieldNames;
    private final String keyDelimiter;
    private final int numKeyFields;

    @Override
    public EnrichmentCommand map(Row row) {

        String enrichmentKey = IntStream.range(0, numKeyFields).mapToObj(index -> Objects.toString(row.getField(index), NULL_STRING_VALUE)).collect(Collectors.joining(keyDelimiter));

        Map<String, String> enrichmentValues = new HashMap<>();
        IntStream.range(numKeyFields, row.getArity()).
                forEach(index -> enrichmentValues.put(fieldNames.get( index - numKeyFields), Objects.toString(row.getField(index), "null")));

        EnrichmentEntry enrichmentEntry = EnrichmentEntry.builder().ts(MessageUtils.getCurrentTimestamp()).
                type(enrichmentType).
                key(enrichmentKey).entries(enrichmentValues).build();

        log.info("Writing enrichment key {}", enrichmentKey);

        return EnrichmentCommand.builder().
                headers(Collections.emptyMap()).
                type(commandType).payload(enrichmentEntry).build();
    }
}
