package com.cloudera.cyber.indexing;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.cloudera.cyber.indexing.MappingColumnDto.EXTENSIONS_PATH;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MappingColumnDtoTest {

    @Test
    public void testGetKafkaNameList() {
        testGetKafkaNameList("kafka_name", null, null, List.of("['kafka_name']"));
        testGetKafkaNameList("kafka_name", null, false,  List.of(".kafka_name"));
        testGetKafkaNameList("name1,name2", null, false,  List.of(".name1", ".name2"));

        testGetKafkaNameList("kafka_name", "..", null, List.of("kafka_name"));
        testGetKafkaNameList("kafka_name", "..", false,  List.of("kafka_name"));
        testGetKafkaNameList("name1,name2", "..", false,  List.of("name1", "name2"));

    }

    private void testGetKafkaNameList(String kafkaName, String path, Boolean isMap, List<String> expectedResult) {
        MappingColumnDto dto = new MappingColumnDto("name", kafkaName, path, null, isMap);
        assertEquals(expectedResult, dto.getKafkaNameList());
        assertEquals(kafkaName, dto.getRawKafkaName());
    }

    @Test
    public void testGetPath() {
        testGetPath(null, EXTENSIONS_PATH);
        testGetPath(".", "");
        testGetPath("..", "..");
    }

    private void testGetPath(String path, String expectedResult) {
       MappingColumnDto dto = new MappingColumnDto("name", "kafka_name", path, null, null);
       assertEquals(expectedResult, dto.getPath());
       assertEquals(path, dto.getRawPath());
    }

    @Test
    public void testGetIsMap() {
        testGetIsMap(null, null, true);
        testGetIsMap(null, Boolean.FALSE, false);
        testGetIsMap(null, Boolean.TRUE, true);
        testGetIsMap(".", null, false);
        testGetIsMap(".", Boolean.FALSE, false);
        testGetIsMap(".", Boolean.TRUE, true);
    }

    private void testGetIsMap(String path, Boolean isMap, boolean expectedResult) {
        MappingColumnDto dto = new MappingColumnDto("name", "kafka_name", path, null, isMap);
        assertEquals(expectedResult, dto.getIsMap());
    }

    @Test
    public void testKafkaColumn() {
        String columnName = "column_name";
        String kafkaName = "kafka_name";
        String unscopedEnrich = "unscoped_enriched_field";
        String scopedEnrich = "scoped_enriched";

        MappingColumnDto expectedMapping = new MappingColumnDto(columnName, kafkaName, null, "transform", Boolean.FALSE);
        List<String> unscopedEnrichments = List.of(unscopedEnrich);
        Set<String> scopedEnrichments = Set.of(scopedEnrich);
        // don't filter extensions that are not in enrichment lists
        testDeriveUnenriched(expectedMapping, columnName, kafkaName, unscopedEnrichments, scopedEnrichments);
        // filter extensions that are either unscoped (geo, company,etc) or scoped to a source (hbase or local)
        testDeriveUnenriched(null, columnName, unscopedEnrich, unscopedEnrichments, scopedEnrichments);
        testDeriveUnenriched(null, columnName, scopedEnrich, unscopedEnrichments, scopedEnrichments);

        String nonExtensionsPath = "..";
        // only filter enrichments in the path extensions - names in the enrichment lists are only removed if they appear in extensions
        expectedMapping = new MappingColumnDto(columnName, unscopedEnrich, nonExtensionsPath, "transform", Boolean.FALSE);
        testDeriveUnenriched(expectedMapping, columnName, unscopedEnrich, nonExtensionsPath, unscopedEnrichments, scopedEnrichments);
        expectedMapping = new MappingColumnDto(columnName, scopedEnrich, nonExtensionsPath, "transform", Boolean.FALSE);
        testDeriveUnenriched(expectedMapping, columnName, scopedEnrich, nonExtensionsPath, unscopedEnrichments, scopedEnrichments);
    }

    @Test
    public void testColumnNameOnly() {
        String columnName = "column_name";
        String unscopedEnrich = "unscoped_enriched_field";
        String scopedEnrich = "scoped_enriched";

        MappingColumnDto expectedMapping = new MappingColumnDto(columnName, null, null, "transform", Boolean.FALSE);
        List<String> unscopedEnrichments = List.of(unscopedEnrich);
        Set<String> scopedEnrichments = Set.of(scopedEnrich);
        // don't filter extensions that are not in enrichment lists
        testDeriveUnenriched(expectedMapping, columnName, null, unscopedEnrichments, scopedEnrichments);
        // filter extensions that are either unscoped (geo, company,etc) or scoped to a source (hbase or local)
        testDeriveUnenriched(null, unscopedEnrich, null, unscopedEnrichments, scopedEnrichments);
        testDeriveUnenriched(null, scopedEnrich, null, unscopedEnrichments, scopedEnrichments);

        String nonExtensionsPath = "..";
        // only filter enrichments in the path extensions - names in the enrichment lists are only removed if they appear in extensions
        expectedMapping = new MappingColumnDto(unscopedEnrich, null, nonExtensionsPath, "transform", Boolean.FALSE);
        testDeriveUnenriched(expectedMapping, unscopedEnrich, null, nonExtensionsPath, unscopedEnrichments, scopedEnrichments);
        expectedMapping = new MappingColumnDto(scopedEnrich, null, nonExtensionsPath, "transform", Boolean.FALSE);
        testDeriveUnenriched(expectedMapping, scopedEnrich, null, nonExtensionsPath, unscopedEnrichments, scopedEnrichments);
    }

    private void testDeriveUnenriched(MappingColumnDto expectedResult, String columnName, String kafkaName, List<String> unscopedEnrichments, Set<String> scopedEnrichments) {
        testDeriveUnenriched(expectedResult, columnName, kafkaName, null, unscopedEnrichments, scopedEnrichments);
    }

    private void testDeriveUnenriched(MappingColumnDto expectedResult, String columnName, String kafkaName, String path, List<String> unscopedEnrichments, Set<String> scopedEnrichments) {
        MappingColumnDto dtoToTest = new MappingColumnDto(columnName, kafkaName, path, "transform", Boolean.FALSE);
        assertEquals(expectedResult, dtoToTest.deriveUnenriched(unscopedEnrichments, scopedEnrichments));
    }
}
