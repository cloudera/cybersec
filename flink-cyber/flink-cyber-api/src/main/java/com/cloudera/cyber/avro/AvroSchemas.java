package com.cloudera.cyber.avro;

import org.apache.avro.SchemaBuilder;

public class AvroSchemas {

    public static SchemaBuilder.RecordBuilder<org.apache.avro.Schema> createRecordBuilder(String namespace, String recordName) {
        return SchemaBuilder.record(recordName).namespace(namespace)
                .prop("ssb.rowtimeAttribute", "ts")
                .prop("ssb.watermarkExpression", "`ts` - INTERVAL '30' SECOND");
    }
}
