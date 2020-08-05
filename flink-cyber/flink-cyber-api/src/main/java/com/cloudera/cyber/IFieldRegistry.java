package com.cloudera.cyber;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

public interface IFieldRegistry {

    void addSchema(SchemaBuilder.FieldAssembler<Schema> builder);

    boolean contains(String key);
}
