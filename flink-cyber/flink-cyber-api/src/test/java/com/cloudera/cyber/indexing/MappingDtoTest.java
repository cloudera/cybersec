package com.cloudera.cyber.indexing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MappingDtoTest {
    private static final String FIELD_1_NAME = "field1";
    private static final MappingColumnDto FIELD_1_MAPPING = new MappingColumnDto(FIELD_1_NAME, null, null, null, null);
    private static final String FIELD_2_NAME = "field2";
    private static final String FIELD_2_KAFKA_NAME = "field2_kafka_name";
    private static final MappingColumnDto FIELD_2_MAPPING = new MappingColumnDto(FIELD_2_NAME, FIELD_2_KAFKA_NAME, null, null, null);
    private static final String FIELD_3_NAME = "field3";
    private static final String FIELD_4_NAME = "field4";
    private static final String IGNORED_FIELD_NAME = "ignored";
    private static final String TABLE_NAME = "table";
    private static final String UNENRICHED_TABLE_NAME = TABLE_NAME.concat("raw");

    @Test
    public void testDeserializeFromJson() throws IOException {
        InputStream resourceStream = getClass().getClassLoader().getResourceAsStream("mapping-config.json");
        ObjectMapper mapper = new ObjectMapper();
        Map<String, MappingDto> tableDtoMap =  mapper.readValue(resourceStream, new TypeReference<>() {
        });

        java.util.List<String> nameList = tableDtoMap.get("squid").getColumnMapping().get(0).getKafkaNameList();
        assertEquals("['domain']", nameList.get(0));
    }

    @Test
    public void testDeriveUnenrichedMapping() {
        MappingDto mapping = createMapping();

        MappingDto derivedMapping = mapping.deriveUnenrichedMapping(UNENRICHED_TABLE_NAME, List.of(FIELD_2_NAME), Collections.emptySet());
        assertEquals(new MappingDto(UNENRICHED_TABLE_NAME, List.of(IGNORED_FIELD_NAME), List.of(FIELD_1_MAPPING)), derivedMapping);

        derivedMapping = mapping.deriveUnenrichedMapping(UNENRICHED_TABLE_NAME, Collections.emptyList(), Set.of(FIELD_2_NAME));
        assertEquals(new MappingDto(UNENRICHED_TABLE_NAME, List.of(IGNORED_FIELD_NAME), List.of(FIELD_1_MAPPING)), derivedMapping);

        derivedMapping = mapping.deriveUnenrichedMapping(UNENRICHED_TABLE_NAME, Collections.emptyList(), Collections.emptySet());
        assertEquals(new MappingDto(UNENRICHED_TABLE_NAME, List.of(IGNORED_FIELD_NAME), List.of(FIELD_1_MAPPING, FIELD_2_MAPPING)), derivedMapping);

        derivedMapping = mapping.deriveUnenrichedMapping(UNENRICHED_TABLE_NAME, List.of(FIELD_1_NAME), Set.of(FIELD_2_NAME));
        assertNull(derivedMapping);
    }

    private static MappingDto createMapping() {
        List<MappingColumnDto> columns = List.of(FIELD_1_MAPPING, FIELD_2_MAPPING);

        return new MappingDto("table", List.of(IGNORED_FIELD_NAME), columns);
    }

    @Test
    public void testGetIgnoredFields() {
        MappingDto dto = new MappingDto(TABLE_NAME, null, List.of(FIELD_1_MAPPING, FIELD_2_MAPPING));
        assertEquals(Set.of(""), dto.getIgnoreFields());

        dto.setIgnoreFields(List.of(FIELD_3_NAME));
        assertEquals(Set.of(FIELD_2_KAFKA_NAME, FIELD_1_NAME, FIELD_3_NAME), dto.getIgnoreFields());

        // add mapping to field outside extensions path - not added to ignored fields
        dto.setColumnMapping(List.of(FIELD_1_MAPPING, FIELD_2_MAPPING, new MappingColumnDto(FIELD_4_NAME, null, "..", null, false)));
        assertEquals(Set.of(FIELD_2_KAFKA_NAME, FIELD_1_NAME, FIELD_3_NAME), dto.getIgnoreFields());
    }

}
