package com.cloudera.cyber;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;

public class AvroTypes {
    public static final Schema timestampMilliType = LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG));
    public static final Schema uuidType = LogicalTypes.uuid().addToSchema(Schema.create(Schema.Type.STRING));

}
