package com.cloudera.cyber.indexing.hive.tableapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloudera.cyber.indexing.MappingColumnDto;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.TableDto;
import com.cloudera.cyber.indexing.hive.tableapi.impl.TableApiKafkaJob;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TableApiAbstractJobTest {

  public static final String GIVEN_SOURCE = "source";
  public static final String GIVEN_TABLE_NAME = "tableName";
  private final TableApiAbstractJob job = new TableApiKafkaJob(null, null, null, null);

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
                    new MappingColumnDto("column9", null, null, null, false))))),

        Arguments.of(Collections.singletonMap(GIVEN_TABLE_NAME, ResolvedSchema.of(
                Column.physical("column1", DataTypes.STRING()),
                Column.physical("column2", DataTypes.STRING()),
                Column.physical("column3", DataTypes.STRING()))),
            Collections.singletonMap(GIVEN_SOURCE,
                new MappingDto(GIVEN_TABLE_NAME, new ArrayList<>(), Arrays.asList(
                    new MappingColumnDto("column1", "column1,column2", null, null, false),
                    new MappingColumnDto("column2", "column3", null, null, false))))));
  }

  public static Stream<Arguments> insertSqlData() {
    return Stream.of(
          Arguments.of("topic",
                new MappingDto("tableName", Collections.emptyList(), Collections.emptyList()),
                ResolvedSchema.of(),
                "CREATE TEMPORARY VIEW  topic_tmpview(  )  AS \n" +
                " SELECT   \n" +
                " from KafkaTempView\n" +
                " where `source`='topic'"),
          Arguments.of("topic",
                new MappingDto("tableName", Collections.emptyList(), Arrays.asList(
                      new MappingColumnDto("column1", "column1,column2", null, "ROW(%s, %s)", false),
                      new MappingColumnDto("column2", "column3", null, null, false))),
                ResolvedSchema.of(
                      Column.physical("column1", DataTypes.STRING()),
                      Column.physical("column2", DataTypes.STRING()),
                      Column.physical("column3", DataTypes.STRING())),
                "CREATE TEMPORARY VIEW  topic_tmpview( column1, column2 )  AS \n" +
                " SELECT  ROW((message.extensions.column1), (message.extensions.column2)), (message.extensions.column3) \n" +
                " from KafkaTempView\n" +
                " where `source`='topic'"),
          Arguments.of("topic",
                new MappingDto("tableName", Collections.emptyList(), Arrays.asList(
                      new MappingColumnDto("fields", "extensions", ".", "filterMap(%s)", false))),
                ResolvedSchema.of(
                      Column.physical("fields", DataTypes.MAP(DataTypes.STRING(), DataTypes.STRING()))),
                "CREATE TEMPORARY VIEW  topic_tmpview( fields )  AS \n" +
                " SELECT  filterMaptopictableName((message.extensions)) \n" +
                " from KafkaTempView\n" +
                " where `source`='topic'")
    );
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
                new MappingDto(GIVEN_TABLE_NAME,  new ArrayList<>(), Arrays.asList(
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
  @MethodSource("insertSqlData")
  void shouldGenerateInsertSql(String topic, MappingDto mappingDto, ResolvedSchema tableSchema, String expectedSql) {
      String actualSql = job.buildInsertSql(topic, mappingDto, tableSchema);
      assertThat(actualSql).isEqualTo(expectedSql);
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

  @Test
  void selectMappingsKeepsEverythingWhenNoSourcesRequested() {
    Map<String, MappingDto> mappings = new HashMap<>();
    mappings.put("squid", new MappingDto("examples_full_events", Collections.emptyList(), Collections.emptyList()));
    mappings.put("dns", new MappingDto("dns_table", Collections.emptyList(), Collections.emptyList()));

    assertThat(job.selectMappings(mappings, null)).isSameAs(mappings);
  }

  @Test
  void selectMappingsKeepsOnlyRequestedSources() {
    Map<String, MappingDto> mappings = new HashMap<>();
    mappings.put("squid", new MappingDto("examples_full_events", Collections.emptyList(), Collections.emptyList()));
    mappings.put("dns", new MappingDto("dns_table", Collections.emptyList(), Collections.emptyList()));

    assertThat(job.selectMappings(mappings, new HashSet<>(Set.of("squid"))))
        .containsOnlyKeys("squid");
  }

  @Test
  void selectTablesKeepsOnlyTablesReferencedByMappings() {
    Map<String, TableDto> tables = new HashMap<>();
    tables.put("examples_full_events", new TableDto(null, Collections.emptyList()));
    tables.put("examples_full_events_raw", new TableDto(null, Collections.emptyList()));
    tables.put("dns_table", new TableDto(null, Collections.emptyList()));

    Map<String, MappingDto> topicMapping = new HashMap<>();
    topicMapping.put("squid", new MappingDto("examples_full_events", Collections.emptyList(), Collections.emptyList()));
    Map<String, MappingDto> unenrichedTopicMapping = new HashMap<>();
    unenrichedTopicMapping.put("squid", new MappingDto("examples_full_events_raw", Collections.emptyList(), Collections.emptyList()));

    assertThat(job.selectTables(tables, topicMapping, unenrichedTopicMapping))
        .containsOnlyKeys("examples_full_events", "examples_full_events_raw");
  }

  @Test
  void selectTablesDropsUnenrichedTablesWhenTheyAreNotWritten() {
    Map<String, TableDto> tables = new HashMap<>();
    tables.put("examples_full_events", new TableDto(null, Collections.emptyList()));
    tables.put("examples_full_events_raw", new TableDto(null, Collections.emptyList()));

    Map<String, MappingDto> topicMapping = new HashMap<>();
    topicMapping.put("squid", new MappingDto("examples_full_events", Collections.emptyList(), Collections.emptyList()));

    assertThat(job.selectTables(tables, topicMapping, Collections.emptyMap()))
        .containsOnlyKeys("examples_full_events");
  }
}