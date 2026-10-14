package com.cloudera.cyber.indexing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableDtoTest {
    private static final String TABLE_NAME = "table";
    private static final String UNENRICHED_TABLE_NAME = TABLE_NAME.concat("raw");
    private static final String FIELD_1_NAME = "field1";
    private static final String FIELD_2_NAME = "field2";
    private static final MappingColumnDto FIELD_1_MAPPING = new MappingColumnDto(FIELD_1_NAME, null, null, null, null);
    private static final String FIELD_2_KAFKA_NAME = "field2_kafka_name";
    private static final MappingColumnDto FIELD_2_MAPPING = new MappingColumnDto(FIELD_2_NAME, FIELD_2_KAFKA_NAME, null, null, null);
    private static final String LOG_SOURCE_1 = "source1";
    private static final String LOG_SOURCE_2 = "source2";
    private static final Map<String, MappingDto> ORIGINAL_MAPPING = Map.of(LOG_SOURCE_1, new MappingDto(TABLE_NAME, null, List.of(FIELD_1_MAPPING, FIELD_2_MAPPING)),
                                                                           LOG_SOURCE_2, new MappingDto(TABLE_NAME, null, List.of(FIELD_2_MAPPING)),
                                                                           "other_source", new MappingDto("different_table", null, List.of(FIELD_1_MAPPING)));
    private static final TableColumnDto TABLE_FIELD_1 = new TableColumnDto(FIELD_1_NAME, "String", true);

    @Test
    public void testDeriveUnenrichedTable() {
        Map<String, MappingDto> unenrichedMapping = new HashMap<>();
        TableDto tableDto = createTable(UNENRICHED_TABLE_NAME);

        List<String> enrichmentPrefixes = List.of(FIELD_2_KAFKA_NAME);
        TableDto expectedUnenrichedTable = new TableDto(null, List.of(TABLE_FIELD_1));
        Map<String, MappingDto> expectedUnenrichedMapping = Map.of(LOG_SOURCE_1, new MappingDto(UNENRICHED_TABLE_NAME, null, List.of(FIELD_1_MAPPING)));
        TableDto unenrichedTable = tableDto.deriveUnenrichedTable(TABLE_NAME, ORIGINAL_MAPPING, unenrichedMapping, enrichmentPrefixes, Collections.emptyMap());

        assertEquals(expectedUnenrichedTable, unenrichedTable);
        assertEquals(expectedUnenrichedMapping, unenrichedMapping);

        unenrichedMapping = new HashMap<>();
        unenrichedTable = tableDto.deriveUnenrichedTable(TABLE_NAME, ORIGINAL_MAPPING, unenrichedMapping, enrichmentPrefixes,
                Map.of(LOG_SOURCE_1, Set.of(FIELD_1_NAME),
                LOG_SOURCE_2, Set.of(FIELD_1_NAME)));
        assertNull(unenrichedTable);
        assertTrue(unenrichedMapping.isEmpty());
    }

    private static TableDto createTable(String unenrichedTableName) {
        List<TableColumnDto> columns = List.of(TABLE_FIELD_1,
                new TableColumnDto(FIELD_2_NAME, "Integer", false));

        return new TableDto(unenrichedTableName, columns);
    }

    @Test
    public void testNoDerivedTable() {
        TableDto tableDto = createTable(null);
        Map<String, MappingDto> unenrichedMapping = new HashMap<>();
        assertNull(tableDto.deriveUnenrichedTable(TABLE_NAME, ORIGINAL_MAPPING, unenrichedMapping, List.of(FIELD_2_KAFKA_NAME), Collections.emptyMap()));
        assertTrue(unenrichedMapping.isEmpty());
    }

    @Test
    public void testDeserializeFromJson() throws IOException {
        testReadTableDto("table-config-legacy.json", null, 2);
        testReadTableDto("table-config-new.json", "examples_full_events_raw", 2);
        testReadTableDto("table-config-missing-field.json", "examples_full_events_raw", 0);
        testReadTableDto("table-config-no-unenriched.json", null, 2);
        testReadTableDto("table-config-unknown-field.json", null, 0);
    }

    @Test
    public void testDeserializeFromBadJson() {
        Exception actualException = assertThrows(JsonProcessingException.class, () -> {
            InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("table-config-bad-syntax.json");
            ObjectMapper mapper = new ObjectMapper();
            mapper.readValue(resourceStream, new TypeReference<Map<String, TableDto>>() {
            });
        });
        assertTrue(actualException.getMessage().startsWith("Unexpected end-of-input"));

    }

    private void testReadTableDto(String resourceName, String unenrichedTableName, int expectedColumns) throws IOException {
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourceName);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, TableDto> tableDtoMap =  mapper.readValue(resourceStream, new TypeReference<>() {
        });
        TableDto tableDto = tableDtoMap.get("examples_full_events");
        assertNotNull(tableDto);
        assertEquals(unenrichedTableName, tableDto.getUnenrichedTableName());
        if (expectedColumns > 0) {
            assertEquals(2, tableDto.getColumns().size());
        } else {
            assertNull(tableDto.getColumns());
        }
    }

}
