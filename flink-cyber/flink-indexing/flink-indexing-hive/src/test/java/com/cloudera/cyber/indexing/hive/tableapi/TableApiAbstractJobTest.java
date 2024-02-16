package com.cloudera.cyber.indexing.hive.tableapi;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.cloudera.cyber.indexing.MappingColumnDto;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.hive.tableapi.impl.TableApiKafkaJob;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TableApiAbstractJobTest {

  public static final String GIVEN_SOURCE = "source";
  public static final String GIVEN_TABLE_NAME = "tableName";
  private final TableApiAbstractJob job = new TableApiKafkaJob(null, null, null);

  TableApiAbstractJobTest() throws IOException {
  }

  public static Stream<Arguments> mappingsData() {
    return Stream.of(
        Arguments.of(new HashMap<>(), new HashMap<>()),

        Arguments.of(Collections.singletonMap(GIVEN_TABLE_NAME, ResolvedSchema.of(
                Column.physical("column1", DataTypes.STRING()),
                Column.physical("column2", DataTypes.STRING()),
                Column.physical("column3", DataTypes.INT()),
                Column.physical("column4", DataTypes.BOOLEAN()),
                Column.physical("column5", DataTypes.BIGINT()),
                Column.physical("column6", DataTypes.DOUBLE()),
                Column.physical("column7", DataTypes.FLOAT()),
                Column.physical("column8", DataTypes.SMALLINT()),
                Column.physical("column9", DataTypes.TINYINT()))),
            Collections.singletonMap(GIVEN_SOURCE,
                new MappingDto(GIVEN_TABLE_NAME, new ArrayList<>(), Arrays.asList(
                    new MappingColumnDto("column1", null, null, null, false),
                    new MappingColumnDto("column2", null, null, null, false),
                    new MappingColumnDto("column3", null, null, null, false),
                    new MappingColumnDto("column4", null, null, null, false),
                    new MappingColumnDto("column5", null, null, null, false),
                    new MappingColumnDto("column6", null, null, null, false),
                    new MappingColumnDto("column7", null, null, null, false),
                    new MappingColumnDto("column8", null, null, null, false),
                    new MappingColumnDto("column9", null, null, null, false))))));
  }

  public static Stream<Arguments> mappingsExceptionData() {
    return Stream.of(
        Arguments.of(new HashMap<>(),
            Collections.singletonMap(GIVEN_SOURCE,
                new MappingDto(" ", new ArrayList<>(), new ArrayList<>())),
            RuntimeException.class,
            String.format("Provided empty table name for the [%s] source!", GIVEN_SOURCE)),

        Arguments.of(new HashMap<>(),
            Collections.singletonMap(GIVEN_SOURCE,
                new MappingDto(GIVEN_TABLE_NAME, new ArrayList<>(), new ArrayList<>())),
            RuntimeException.class,
            String.format("Table [%s] is not found!", GIVEN_TABLE_NAME)),

        Arguments.of(Collections.singletonMap(GIVEN_TABLE_NAME, ResolvedSchema.of()),
            Collections.singletonMap(GIVEN_SOURCE,
                new MappingDto(GIVEN_TABLE_NAME, new ArrayList<>(), Arrays.asList(
                    new MappingColumnDto(" ", null, null, null, false),
                    new MappingColumnDto("someName", null, null, null, false)))),
            RuntimeException.class,
            String.format(
                "Found invalid column mappings for source [%s]. Those columns are either not present in the table config or have empty names: %s",
                GIVEN_SOURCE, "[ , someName]")),

        Arguments.of(Collections.singletonMap(GIVEN_TABLE_NAME, ResolvedSchema.of(
                Column.physical("column1", DataTypes.DATE()),
                Column.physical("column2", DataTypes.DATE()),
                Column.physical("column3", DataTypes.STRING()))),
            Collections.singletonMap(GIVEN_SOURCE,
                new MappingDto(GIVEN_TABLE_NAME, new ArrayList<>(), Arrays.asList(
                    new MappingColumnDto("column1", null, null, null, false),
                    new MappingColumnDto("column2", null, null, "someTransformation", false),
                    new MappingColumnDto("column3", null, null, null, false)))),
            RuntimeException.class,
            String.format(
                "Found column mappings of non-string type without transformations for source [%s]: %s",
                GIVEN_SOURCE, "[column1]")));
  }
  @ParameterizedTest
  @MethodSource("mappingsData")
  void shouldValidateMappings(Map<String, ResolvedSchema> givenTableSchemaMap,
      Map<String, MappingDto> givenTopicMapping) {
    job.validateMappings(givenTableSchemaMap, givenTopicMapping);
  }

  @ParameterizedTest
  @MethodSource("mappingsExceptionData")
  void shouldThrowExceptionWhenValidateMappings(Map<String, ResolvedSchema> givenTableSchemaMap,
      Map<String, MappingDto> givenTopicMapping, Class<? extends Exception> expectedException,
      String expectedExceptionMessage) {
    assertThatThrownBy(() -> job.validateMappings(givenTableSchemaMap, givenTopicMapping))
        .isInstanceOf(expectedException)
        .hasMessage(expectedExceptionMessage);
  }
}