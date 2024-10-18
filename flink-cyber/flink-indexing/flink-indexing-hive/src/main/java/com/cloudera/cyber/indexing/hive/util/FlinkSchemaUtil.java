package com.cloudera.cyber.indexing.hive.util;

import com.cloudera.cyber.indexing.TableColumnDto;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.types.DataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class FlinkSchemaUtil {

    public static Schema buildSchema(ResolvedSchema resolvedSchema) {
        return Schema.newBuilder()
            .fromResolvedSchema(resolvedSchema)
            .build();
    }

    public static ResolvedSchema getResolvedSchema(List<TableColumnDto> columnList) {
        final List<Column> flinkColumnList = columnList.stream()
                .map(col -> Column.physical(col.getName(), getFlinkType(col.getType(), col.getNullable())))
                .collect(Collectors.toList());
        return ResolvedSchema.of(flinkColumnList);
    }

    private static DataType getFlinkType(String colType) {
        return getFlinkType(colType, true);
    }

    /**
     * Creates Flink Data Type from the config column type.
     *
     * @param colType  config column type.
     *                 Possible config column type values are:
     *                 string, timestamp, date, int, bigint, float, double, boolean, bytes, null,
     *                 array<type>, map<key,value>, struct<field:type, field2:type2>
     * @param nullable whether column is nullable or not
     * @return Flink DataType that describes provided column type
     */
    private static DataType getFlinkType(String colType, Boolean nullable) {
        if (colType == null) {
            throw new IllegalArgumentException("Column type cannot be null");
        }
        final String type = colType.toLowerCase().trim();
        DataType result;
        if (type.equals("string")) {
            result = DataTypes.STRING();
        } else if (type.equals("timestamp")) {
            result = DataTypes.TIMESTAMP(9);
        } else if (type.equals("date")) {
            result = DataTypes.DATE();
        } else if (type.equals("int")) {
            result = DataTypes.INT();
        } else if (type.equals("bigint")) {
            result = DataTypes.BIGINT();
        } else if (type.equals("float")) {
            result = DataTypes.FLOAT();
        } else if (type.equals("double")) {
            result = DataTypes.DOUBLE();
        } else if (type.equals("boolean")) {
            result = DataTypes.BOOLEAN();
        } else if (type.equals("bytes")) {
            result = DataTypes.BYTES();
        } else if (type.equals("null")) {
            result = DataTypes.NULL();
        } else if (type.startsWith("array")) {
            result = parseArrayType(type);
        } else if (type.startsWith("map")) {
            result = parseMapType(type);
        } else if (type.startsWith("struct")) {
            result = parseStructType(type);
        } else {
            throw new IllegalArgumentException("Unknown column type: " + type);
        }
        return Optional.ofNullable(nullable).orElse(true) ? result.nullable() : result.notNull();
    }

    /**
     * Parses Array type from the config column type.
     *
     * @param type config column type. Supported format is: array<type>
     * @return Flink DataType that describes provided array type
     */
    private static DataType parseArrayType(String type) {
        final String body = getInnerBody(type, "Array");
        return DataTypes.ARRAY(getFlinkType(body));
    }

    /**
     * Parses Map type from the config column type.
     *
     * @param type config column type. Supported format is: map<key,value>
     * @return Flink DataType that describes provided map type
     */
    private static DataType parseMapType(String type) {
        final String body = getInnerBody(type, "Map");
        if (!body.contains(",")) {
            throw new IllegalArgumentException("Unknown column type for Map: " + type);
        }
        final List<String> split = splitTypes(body, ',');
        return DataTypes.MAP(getFlinkType(split.get(0)), getFlinkType(split.get(1)));
    }

    /**
     * Parses Struct type from the config column type.
     *
     * @param type config column type. Supported format is: struct<name:type, name2:type2>. The name can contain only alphanumeric characters and underscores.
     * @return Flink DataType that describes provided struct type
     */
    private static DataType parseStructType(String type) {
        final String body = getInnerBody(type, "Struct");
        if (!body.contains(":")) {
            throw new IllegalArgumentException("Unknown column type for Struct: " + type);
        }
        final List<String> fields = splitTypes(body, ',');
        final List<DataTypes.Field> fieldList = new ArrayList<>();
        for (String field : fields) {
            final List<String> fieldSplit = splitTypes(field, ':');
            if (fieldSplit.size() < 2) {
                throw new IllegalArgumentException("Unknown column type for Struct: " + type);
            }
            final String name = fieldSplit.get(0).replaceAll("[^a-zA-Z0-9_]", "");
            final DataType value = getFlinkType(fieldSplit.get(1));
            fieldList.add(DataTypes.FIELD(name, value));
        }
        return DataTypes.ROW(fieldList.toArray(new DataTypes.Field[0]));
    }

    private static String getInnerBody(String type, String typeName) {
        final int start = type.indexOf("<");
        final int stop = type.lastIndexOf(">");
        if (start < 0 || stop < 0) {
            throw new IllegalArgumentException("Unknown column type for " + typeName + " : " + type);
        }
        return type.substring(start + 1, stop);
    }

    private static List<String> splitTypes(String body, char delimiter) {
        List<String> types = new ArrayList<>();
        int start = 0;
        int end = 0;
        int count = 0;
        while (end < body.length()) {
            if (body.charAt(end) == '<') {
                count++;
            } else if (body.charAt(end) == '>') {
                count--;
            } else if (body.charAt(end) == delimiter && count == 0) {
                types.add(body.substring(start, end));
                start = end + 1;
            }
            end++;
        }
        types.add(body.substring(start, end));
        return types;
    }
}
