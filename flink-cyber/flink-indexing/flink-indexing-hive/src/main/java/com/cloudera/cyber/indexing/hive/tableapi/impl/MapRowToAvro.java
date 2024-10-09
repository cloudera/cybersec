package com.cloudera.cyber.indexing.hive.tableapi.impl;

import com.cloudera.cyber.indexing.hive.util.AvroSchemaUtil;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.formats.avro.typeutils.GenericRecordAvroTypeInfo;
import org.apache.flink.types.Row;

import java.util.Set;

public class MapRowToAvro implements ResultTypeQueryable<GenericRecord>, MapFunction<Row, GenericRecord> {
    private final GenericRecordAvroTypeInfo producedType;
    private final Schema schema;

    public MapRowToAvro(String schemaString) {
        this.schema = new Schema.Parser().parse(schemaString);
        this.producedType = new GenericRecordAvroTypeInfo(schema);
    }

    @Override
    public TypeInformation<GenericRecord> getProducedType() {
        return producedType;
    }

    @Override
    public GenericRecord map(Row row) throws Exception {
        final GenericRecord record = new GenericData.Record(schema);
        final Set<String> fieldNames = row.getFieldNames(true);
        if (fieldNames != null) {
            for (String fieldName : fieldNames) {
                AvroSchemaUtil.putRowIntoAvro(row, record, fieldName);
            }
        }
        return record;
    }
}
