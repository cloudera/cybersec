package com.cloudera.cyber.indexing.hive.util;

import com.cloudera.cyber.avro.AvroSchemas;
import com.cloudera.cyber.indexing.TableColumnDto;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.Row;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AvroSchemaUtil {

    //method that converts from flink Schema to avro Schema
    public static Schema convertToAvro(List<TableColumnDto> tableColumnList) {
        return convertToAvro(FlinkSchemaUtil.getResolvedSchema(tableColumnList));
    }

    //method that converts from flink Schema to avro Schema
    public static Schema convertToAvro(ResolvedSchema schema) {
        SchemaBuilder.FieldAssembler<Schema> fieldAssembler = AvroSchemas.createRecordBuilder("com.cloudera.cyber","base")
                .fields();

        for (Column col : schema.getColumns()) {
            fieldAssembler = fieldAssembler.name(col.getName()).type().optional().type(AvroSchemaUtil.convertTypeToAvro(col.getName(), col.getDataType().getLogicalType()));
        }
        return fieldAssembler.endRecord();
    }

    public static void putRowIntoAvro(Row row, GenericRecord record, String fieldName) {
        final Object value;
        final String avroFieldName = fieldName.toLowerCase();

        if (row == null) {
            value = null;
        } else {
            try {
            value = convertToAvroObject(record.getSchema().getField(avroFieldName).schema(), row.getField(fieldName));
            } catch (Exception e) {
                throw new RuntimeException(String.format("Error converting avro field %s", avroFieldName), e);
            }
        }
        record.put(avroFieldName, value);
        System.out.println("fieldName: " + fieldName + " value: " + value);
    }

    private static Object convertToAvroObject(Schema fieldSchema, Object value) {
        if (value == null) {
            return null;
        } else if (value instanceof Instant) {
            return ((Instant) value).toEpochMilli();
        } else if (value instanceof Row) {
            final Row nestedRow = (Row) value;
            final Set<String> nestedFieldNames = nestedRow.getFieldNames(true);
            final GenericRecord nestedRecord = new GenericData.Record(fieldSchema);
            if (nestedFieldNames != null) {
                for (String nestedFieldName : nestedFieldNames) {
                    final String avroFieldName = nestedFieldName.toLowerCase();
                    final Object nestedValue = convertToAvroObject(fieldSchema.getField(avroFieldName).schema(), nestedRow.getField(nestedFieldName));
                    nestedRecord.put(avroFieldName, nestedValue);
                }
            }
            return nestedRecord;
        } else if (value.getClass().isArray()) {
            //[null, array<>]
            final Schema elementSchema = fieldSchema.getTypes().get(1).getElementType();
            final List<Object> avroList = new ArrayList<>();
            for (Object item : (Object[]) value) {
                avroList.add(convertToAvroObject(elementSchema, item));
            }
            return avroList;
        } else if (value instanceof List) {
            //[null, list<>]
            final Schema elementSchema = fieldSchema.getTypes().get(1).getElementType();
            final List<Object> avroList = new ArrayList<>();
            for (Object item : (List) value) {
                avroList.add(convertToAvroObject(elementSchema, item));
            }
            return avroList;
        } else if (value instanceof Map) {
            //[null, map<>]
            final Schema valueSchema = fieldSchema.getTypes().get(1).getValueType();
            final Map<String, Object> avroMap = new HashMap<>();
            for (Map.Entry<Object, Object> entry : ((Map<Object, Object>) value).entrySet()) {
                avroMap.put(String.valueOf(entry.getKey()), convertToAvroObject(valueSchema, entry.getValue()));
            }
            return avroMap;
        }
        return value;
    }

    private static Schema convertTypeToAvro(String name, LogicalType dataType) {
        switch (dataType.getTypeRoot()) {
            case CHAR:
            case VARCHAR:
                return Schema.create(Schema.Type.STRING);
            case BOOLEAN:
                return Schema.create(Schema.Type.BOOLEAN);
            case INTEGER:
                return Schema.create(Schema.Type.INT);
            case BIGINT:
                return Schema.create(Schema.Type.LONG);
            case FLOAT:
                return Schema.create(Schema.Type.FLOAT);
            case DOUBLE:
                return Schema.create(Schema.Type.DOUBLE);
            case DATE:
                return Schema.create(Schema.Type.INT); // Represented as the number of days since the Unix epoch
            case TIMESTAMP_WITHOUT_TIME_ZONE:
                return LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG));
            case BINARY:
                return Schema.create(Schema.Type.BYTES);
            case NULL:
                return Schema.create(Schema.Type.NULL);
            case ARRAY:
                LogicalType elementDataType = dataType.getChildren().get(0);
                return Schema.createArray(convertTypeToAvro(name + "_array", elementDataType));
            case MAP:
                LogicalType valueDataType = dataType.getChildren().get(1);
                return Schema.createMap(convertTypeToAvro(name + "_map", valueDataType));
            case ROW:
                final List<RowType.RowField> fieldList = ((RowType) dataType).getFields();
                final SchemaBuilder.FieldAssembler<Schema> fieldAssembler = SchemaBuilder.record(name).fields();
                for (RowType.RowField field : fieldList) {
                    fieldAssembler.name(field.getName()).type().optional().type(convertTypeToAvro(name + "_" + field.getName(), field.getType()));
                }
                return fieldAssembler.endRecord();
            default:
                throw new UnsupportedOperationException("Unsupported data type: " + dataType);
        }
    }
}
