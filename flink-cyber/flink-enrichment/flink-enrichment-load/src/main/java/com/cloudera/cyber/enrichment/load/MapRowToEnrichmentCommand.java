package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.types.Row;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@AllArgsConstructor
@Slf4j
public class MapRowToEnrichmentCommand implements MapFunction<Row, EnrichmentCommand> {
    private final String enrichmentType;
    private final CommandType commandType;
    private final List<String> fieldNames;


    @Override
    public EnrichmentCommand map(Row row) {

        String enrichmentKey = row.getField(0).toString();

        Map<String, String> enrichmentValues = new HashMap<>();
        IntStream.range(1, row.getArity()).
                forEach(index -> enrichmentValues.put(fieldNames.get( index - 1), row.getField(index).toString()));

        EnrichmentEntry enrichmentEntry = EnrichmentEntry.builder().ts(MessageUtils.getCurrentTimestamp()).
                type(enrichmentType).
                key(enrichmentKey).entries(enrichmentValues).build();

        log.info("Writing enrichment key {}", enrichmentKey);

        return EnrichmentCommand.builder().
                headers(Collections.emptyMap()).
                type(commandType).payload(enrichmentEntry).build();
    }
}
