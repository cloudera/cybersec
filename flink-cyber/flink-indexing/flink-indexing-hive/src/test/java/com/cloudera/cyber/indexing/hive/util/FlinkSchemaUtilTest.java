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

package com.cloudera.cyber.indexing.hive.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.hive.util.FlinkSchemaUtil.SerializationFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.Schema.UnresolvedPhysicalColumn;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.types.AbstractDataType;
import org.apache.flink.table.types.DataType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link FlinkSchemaUtil}.
 *
 * <p>The class under test only exposes {@code buildSchema} and {@code getResolvedSchema}; the
 * mapping from the textual column type to a Flink {@link DataType} is reached through
 * {@code getResolvedSchema}. The tests therefore assert on the {@link ResolvedSchema} produced by
 * {@code getResolvedSchema}, which keeps every test independent of private implementation details.
 */
class FlinkSchemaUtilTest {

  @Test
  @DisplayName("the implicit public constructor of the utility class is available")
  void shouldExposeImplicitConstructor() {
    assertThat(new FlinkSchemaUtil()).isNotNull();
  }

  @Test
  @DisplayName("buildSchema copies the resolved schema (names and types)")
  void shouldBuildSchemaFromResolvedSchema() {
    ResolvedSchema resolvedSchema = ResolvedSchema.of(
        Column.physical("name", DataTypes.STRING()),
        Column.physical("count", DataTypes.BIGINT().notNull()));

    Schema schema = FlinkSchemaUtil.buildSchema(resolvedSchema);

    assertThat(schemaColumnNames(schema)).containsExactly("name", "count");
    assertThat(schemaColumnDataTypes(schema))
        .containsExactly(DataTypes.STRING(), DataTypes.BIGINT().notNull());
  }

  @Test
  @DisplayName("buildSchema handles an empty resolved schema")
  void shouldBuildEmptySchema() {
    Schema schema = FlinkSchemaUtil.buildSchema(ResolvedSchema.of());

    assertThat(schema.getColumns()).isEmpty();
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("simpleTypeArguments")
  @DisplayName("getResolvedSchema maps every simple config type, regardless of serialization format")
  void shouldResolveSimpleTypes(String description, String configType, DataType expectedType,
      SerializationFormat format) {
    DataType actualType = firstColumnDataType(column("value", configType, true), format);

    assertThat(actualType).isEqualTo(expectedType);
  }

  @Test
  @DisplayName("getResolvedSchema trims and lowercases the config type")
  void shouldNormalizeConfigType() {
    DataType actualType = firstColumnDataType(column("value", "  STRING  ", true), SerializationFormat.AVRO);

    assertThat(actualType).isEqualTo(DataTypes.STRING());
  }

  @Test
  @DisplayName("getResolvedSchema makes a column nullable when nullable flag is null")
  void shouldTreatNullNullableFlagAsNullable() {
    DataType actualType = firstColumnDataType(column("value", "string", null), SerializationFormat.HIVE);

    assertThat(actualType).isEqualTo(DataTypes.STRING().nullable());
  }

  @Test
  @DisplayName("getResolvedSchema makes a column non-null when nullable flag is false")
  void shouldRespectNullableFlag() {
    DataType actualType = firstColumnDataType(column("value", "string", false), SerializationFormat.HIVE);

    assertThat(actualType).isEqualTo(DataTypes.STRING().notNull());
  }

  @Test
  @DisplayName("getResolvedSchema treats 'not null' suffix as non-null even when nullable flag is true")
  void shouldOverrideNullableWithNotNullSuffix() {
    DataType actualType = firstColumnDataType(column("value", "string not null", true), SerializationFormat.ICEBERG);

    assertThat(actualType).isEqualTo(DataTypes.STRING().notNull());
  }

  @ParameterizedTest(name = "timestamp for {0} has precision 6")
  @EnumSource(value = SerializationFormat.class, names = {"AVRO", "ICEBERG"})
  @DisplayName("getResolvedSchema uses timestamp precision 6 for non-Hive formats")
  void shouldUseTimestampPrecisionSixForNonHive(SerializationFormat format) {
    DataType actualType = firstColumnDataType(column("ts", "timestamp", true), format);

    assertThat(actualType).isEqualTo(DataTypes.TIMESTAMP(6));
  }

  @Test
  @DisplayName("getResolvedSchema uses timestamp precision 9 for Hive")
  void shouldUseTimestampPrecisionNineForHive() {
    DataType actualType = firstColumnDataType(column("ts", "timestamp", true), SerializationFormat.HIVE);

    assertThat(actualType).isEqualTo(DataTypes.TIMESTAMP(9));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("collectionTypeArguments")
  @DisplayName("getResolvedSchema parses array, map and struct types, including nested ones")
  void shouldResolveCollectionTypes(String description, String configType, DataType expectedType) {
    DataType actualType = firstColumnDataType(column("value", configType, true), SerializationFormat.HIVE);

    assertThat(actualType).isEqualTo(expectedType);
  }

  @Test
  @DisplayName("getResolvedSchema strips invalid characters from struct field names")
  void shouldSanitizeStructFieldNames() {
    DataType actualType = firstColumnDataType(
        column("value", "struct<a-b:string, d e:float, f#g:boolean>", true), SerializationFormat.HIVE);

    assertThat(actualType).isEqualTo(DataTypes.ROW(
        DataTypes.FIELD("ab", DataTypes.STRING()),
        DataTypes.FIELD("de", DataTypes.FLOAT()),
        DataTypes.FIELD("fg", DataTypes.BOOLEAN())));
  }

  @Test
  @DisplayName("getResolvedSchema preserves column order and name")
  void shouldPreserveColumnOrderAndNames() {
    ResolvedSchema resolvedSchema = FlinkSchemaUtil.getResolvedSchema(Arrays.asList(
        column("first", "string", true),
        column("second", "int", true),
        column("third", "boolean", true)), SerializationFormat.AVRO);

    assertThat(resolvedSchema.getColumnNames()).containsExactly("first", "second", "third");
    assertThat(resolvedSchema.getColumnDataTypes()).containsExactly(
        DataTypes.STRING(), DataTypes.INT(), DataTypes.BOOLEAN());
  }

  @Test
  @DisplayName("getResolvedSchema returns an empty schema for an empty column list")
  void shouldResolveEmptyColumnList() {
    ResolvedSchema resolvedSchema = FlinkSchemaUtil.getResolvedSchema(
        Collections.emptyList(), SerializationFormat.HIVE);

    assertThat(resolvedSchema.getColumnNames()).isEmpty();
    assertThat(resolvedSchema.getColumnDataTypes()).isEmpty();
  }

  @Test
  @DisplayName("getResolvedSchema rejects a null column type")
  void shouldRejectNullColumnType() {
    assertThatThrownBy(() -> FlinkSchemaUtil.getResolvedSchema(
        Collections.singletonList(column("value", null, true)), SerializationFormat.HIVE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Column type cannot be null");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unknownTypeArguments")
  @DisplayName("getResolvedSchema rejects unknown and malformed config types")
  void shouldRejectUnknownTypes(String description, String configType, String expectedMessage) {
    assertThatThrownBy(() -> FlinkSchemaUtil.getResolvedSchema(
        Collections.singletonList(column("value", configType, true)), SerializationFormat.HIVE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(expectedMessage);
  }

  private static Stream<Arguments> simpleTypeArguments() {
    return Stream.of(SerializationFormat.values())
        .flatMap(format -> Stream.of(
            Arguments.of("string -> STRING", "string", DataTypes.STRING(), format),
            Arguments.of("string not null -> STRING NOT NULL", "string not null",
                DataTypes.STRING().notNull(), format),
            Arguments.of("date -> DATE", "date", DataTypes.DATE(), format),
            Arguments.of("int -> INT", "int", DataTypes.INT(), format),
            Arguments.of("bigint -> BIGINT", "bigint", DataTypes.BIGINT(), format),
            Arguments.of("float -> FLOAT", "float", DataTypes.FLOAT(), format),
            Arguments.of("double -> DOUBLE", "double", DataTypes.DOUBLE(), format),
            Arguments.of("boolean -> BOOLEAN", "boolean", DataTypes.BOOLEAN(), format),
            Arguments.of("bytes -> BYTES", "bytes", DataTypes.BYTES(), format),
            Arguments.of("null -> NULL", "null", DataTypes.NULL(), format)));
  }

  private static Stream<Arguments> collectionTypeArguments() {
    return Stream.of(
        Arguments.of("array<string>", "array<string>", DataTypes.ARRAY(DataTypes.STRING())),
        Arguments.of("array<array<int>>", "array<array<int>>",
            DataTypes.ARRAY(DataTypes.ARRAY(DataTypes.INT()))),
        Arguments.of("map<string,int>", "map<string,int>",
            DataTypes.MAP(DataTypes.STRING(), DataTypes.INT())),
        Arguments.of("map with spaces", "map< string , int >",
            DataTypes.MAP(DataTypes.STRING(), DataTypes.INT())),
        Arguments.of("struct<name:string, age:int>", "struct<name:string,age:int>",
            DataTypes.ROW(
                DataTypes.FIELD("name", DataTypes.STRING()),
                DataTypes.FIELD("age", DataTypes.INT()))),
        Arguments.of("nested array in struct", "struct<name:string, tags:array<string>>",
            DataTypes.ROW(
                DataTypes.FIELD("name", DataTypes.STRING()),
                DataTypes.FIELD("tags", DataTypes.ARRAY(DataTypes.STRING())))),
        Arguments.of("nested struct in map", "map<string,struct<a:int, b:boolean>>",
            DataTypes.MAP(DataTypes.STRING(), DataTypes.ROW(
                DataTypes.FIELD("a", DataTypes.INT()),
                DataTypes.FIELD("b", DataTypes.BOOLEAN())))),
        Arguments.of("comma nested inside a map value within a struct",
            "struct<a:map<string,int>, b:string>",
            DataTypes.ROW(
                DataTypes.FIELD("a", DataTypes.MAP(DataTypes.STRING(), DataTypes.INT())),
                DataTypes.FIELD("b", DataTypes.STRING()))),
        Arguments.of("comma nested inside a collection map key",
            "map<array<string>,map<string,int>>",
            DataTypes.MAP(
                DataTypes.ARRAY(DataTypes.STRING()),
                DataTypes.MAP(DataTypes.STRING(), DataTypes.INT()))));
  }

  private static Stream<Arguments> unknownTypeArguments() {
    return Stream.of(
        Arguments.of("unknown scalar type", "varchar", "Unknown column type: varchar"),
        Arguments.of("array without brackets", "array", "Unknown column type for Array : array"),
        Arguments.of("array without a closing bracket", "array<string",
            "Unknown column type for Array : array<string"),
        Arguments.of("map without brackets", "map", "Unknown column type for Map : map"),
        Arguments.of("struct without brackets", "struct", "Unknown column type for Struct : struct"),
        Arguments.of("map with a single type", "map<string>", "Unknown column type for Map: map<string>"),
        Arguments.of("struct without fields", "struct<>", "Unknown column type for Struct: struct<>"),
        Arguments.of("struct field without a type", "struct<a>", "Unknown column type for Struct: struct<a>"),
        Arguments.of("struct field without a separator", "struct<a:string,b>",
            "Unknown column type for Struct: struct<a:string,b>"),
        Arguments.of("struct field without a value", "struct<a:>", "Unknown column type: "));
  }

  private static DataType firstColumnDataType(TableColumnDto column, SerializationFormat format) {
    return FlinkSchemaUtil.getResolvedSchema(Collections.singletonList(column), format)
        .getColumnDataTypes()
        .get(0);
  }

  private static TableColumnDto column(String name, String type, Boolean nullable) {
    return TableColumnDto.builder().name(name).type(type).nullable(nullable).build();
  }

  private static List<String> schemaColumnNames(Schema schema) {
    return schema.getColumns().stream().map(column -> column.getName()).collect(Collectors.toList());
  }

  private static List<AbstractDataType<?>> schemaColumnDataTypes(Schema schema) {
    return schema.getColumns().stream()
        .map(column -> ((UnresolvedPhysicalColumn) column).getDataType())
        .collect(Collectors.toList());
  }
}
