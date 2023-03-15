package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonParser;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.DeserializationContext;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class CsvToEnrichmentCommandDeserializer extends StdDeserializer<EnrichmentCommand> {
    private static final String NULL_STRING_VALUE = "null";
    private final String enrichmentType;
    private final CommandType commandType;
    private final List<String> keyFieldNames;
    private final String keyDelimiter;
    private final List<String> valueFieldNames;


    protected CsvToEnrichmentCommandDeserializer(String enrichmentType, CommandType commandType,
            List<String> keyFieldNames, String keyDelimiter,
            List<String> valueFieldNames) {
        super(EnrichmentCommand.class);
        this.enrichmentType = enrichmentType;
        this.commandType = commandType;
        this.keyFieldNames = keyFieldNames;
        this.keyDelimiter = keyDelimiter;
        this.valueFieldNames = valueFieldNames;
    }


    @Override
    public EnrichmentCommand deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        Map<String, String> enrichmentValues = new HashMap<>();
        valueFieldNames.forEach(valueFieldName -> {
            JsonNode valueFieldValue = node.get(valueFieldName);
            if (valueFieldValue != null) {
                enrichmentValues.put(valueFieldName, valueFieldValue.asText());
            }
        });

        String enrichmentKey = keyFieldNames.stream().map(keyFieldName -> node.get(keyFieldName).asText(NULL_STRING_VALUE)).collect(Collectors.joining(keyDelimiter));

        EnrichmentEntry enrichmentEntry = EnrichmentEntry.builder().ts(MessageUtils.getCurrentTimestamp()).
                type(enrichmentType).
                key(enrichmentKey).entries(enrichmentValues).build();
        return EnrichmentCommand.builder().
                headers(Collections.emptyMap()).
                type(commandType).payload(enrichmentEntry).build();
    }
}
