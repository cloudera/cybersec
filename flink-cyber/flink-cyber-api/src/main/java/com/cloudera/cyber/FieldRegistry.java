package com.cloudera.cyber;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.util.HashMap;
import java.util.Map;

public class FieldRegistry implements IFieldRegistry {

    private Map<String, Field> fields = new HashMap<>();

    private static FieldRegistry instance;

    public static FieldRegistry instance() {
        if(instance == null) {
            instance = new FieldRegistry();
        }
        return instance;
    }


    @Override
    public void addSchema(SchemaBuilder.FieldAssembler<Schema> builder) {

    }

    @Override
    public boolean contains(String key) {
        return fields.containsKey(key);
    }
}
