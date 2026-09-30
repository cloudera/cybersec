package com.cloudera.cyber.indexing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
// Use custom deserializer to preserve backward compatibility
@JsonDeserialize(using = TableDto.TableDtoDeserializer.class)
public class TableDto {
    public static final String UNENRICHED_TABLE_NAME_JSON_NAME = "unenriched_table_name";
    public static final String COLUMNS_JSON_NAME = "columns";
    @JsonProperty(UNENRICHED_TABLE_NAME_JSON_NAME)
    private String unenrichedTableName;

    @JsonProperty(COLUMNS_JSON_NAME)
    private List<TableColumnDto> columns;

    public static class TableDtoDeserializer extends JsonDeserializer<TableDto> {
        @Override
        public TableDto deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.readValueAsTree();
            TableDto tableDto = new TableDto();

            // BACKWARD COMPATIBLE PATH: Handles old JSON arrays `[{"name": "domain"}]`
            if (node.isArray()) {
                List<TableColumnDto> list = p.getCodec().readValue(
                        node.traverse(p.getCodec()),
                        new TypeReference<List<TableColumnDto>>() {}
                );
                tableDto.setColumns(list);
                tableDto.setUnenrichedTableName(null); // Defaults safely since old files don't have it
            }

            // New format: `{"unenriched_table_name": "x", "columns": [...]}`
            else if (node.isObject()) {
                if (node.has(UNENRICHED_TABLE_NAME_JSON_NAME)) {
                    tableDto.setUnenrichedTableName(node.get(UNENRICHED_TABLE_NAME_JSON_NAME).asText());
                }
                if (node.has(COLUMNS_JSON_NAME)) {
                    List<TableColumnDto> list = p.getCodec().readValue(
                            node.get(COLUMNS_JSON_NAME).traverse(p.getCodec()),
                            new TypeReference<List<TableColumnDto>>() {}
                    );
                    tableDto.setColumns(list);
                }
            }

            return tableDto;
        }
    }

    /**
     * Create the Kafka Mappings and table definition for the enriched table name if the table specifies one.
     *
     * @param originalMappings All original kafka mappings.
     * @param enrichmentFieldPrefixes All enriched kafka name prefixes from unscoped enrichments such as geo, company, asn or cidr.
     * @param sourceToEnrichmentFieldPrefixes All enriched kafka name prefixes for enrichments scoped to a source type such as hbase or local enrichments.
     * @return The derived table mapping and kafka column mappings for the derived table.
     */
    public TableDto deriveUnenrichedTable(String enrichedTableName, Map<String, MappingDto>  originalMappings, Map<String, MappingDto> unenrichedMappings, List<String> enrichmentFieldPrefixes, Map<String, Set<String>> sourceToEnrichmentFieldPrefixes) {
        TableDto unenrichedTable = null;
        
        if (unenrichedTableName != null) {
            Map<String, Boolean> columnToUnenriched = columns.stream().collect(Collectors.toMap(TableColumnDto::getName, v -> Boolean.FALSE));
            // create the derived mappings - determines what columns to include in the table
            originalMappings.entrySet().stream().
                    // for each mapping to this derived table
                    filter(entry -> entry.getValue().getTableName().equals(enrichedTableName)).
                    // create mappings for unenriched columns
                            forEach(sourceToMapping -> {
                                MappingDto derivedMapping = sourceToMapping.getValue().deriveUnenrichedMapping( unenrichedTableName, enrichmentFieldPrefixes, sourceToEnrichmentFieldPrefixes.get(sourceToMapping.getKey()));
                                // derivedMapping may be null if the table maps only enriched fields
                                if (derivedMapping != null) {
                                    unenrichedMappings.put(sourceToMapping.getKey(), derivedMapping);
                                    // track which columns should be included in the derived mapping
                                    derivedMapping.getColumnMapping().stream().map(MappingColumnDto::getName).forEach(columnName -> columnToUnenriched.put(columnName, Boolean.TRUE));
                                }
                            });
            // create the list of columns in the derived unenriched table
            List<TableColumnDto> derivedColumns = this.columns.stream().
                    filter(column -> {
                        Boolean isUnenriched = columnToUnenriched.get(column.getName());
                        return isUnenriched != null &&
                                isUnenriched;
                    }).
                    map(column -> new TableColumnDto(column.getName(), column.getType(), column.getNullable())).
                    toList();
            if (!derivedColumns.isEmpty()) {
                unenrichedTable = new TableDto(null, derivedColumns);
            }
        }
        return unenrichedTable;
    }
}
