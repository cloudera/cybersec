package com.cloudera.cyber.avro;

import org.apache.avro.SchemaBuilder;

public class AvroSchemas {

    public static SchemaBuilder.RecordBuilder<org.apache.avro.Schema> createRecordBuilder(String namespace, String recordName) {
        return createRecordBuilder(namespace, recordName, "ts");
    }

    public static SchemaBuilder.RecordBuilder<org.apache.avro.Schema> createRecordBuilder(String namespace, String recordName, String tsFieldName) {
        SchemaBuilder.RecordBuilder<org.apache.avro.Schema> recordBuilder = SchemaBuilder.record(recordName).namespace(namespace);
        if (tsFieldName != null) {
            recordBuilder
                    .prop("ssb.rowtimeAttribute", tsFieldName)
                    .prop("ssb.watermarkExpression", String.format("`%s` - INTERVAL '30' SECOND", tsFieldName));
        }
        return recordBuilder;
    }
}
