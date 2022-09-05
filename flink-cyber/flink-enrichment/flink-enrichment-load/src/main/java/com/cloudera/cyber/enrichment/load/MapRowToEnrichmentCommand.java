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
