package com.cloudera.cyber.indexing;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.List;

@Data
@EqualsAndHashCode
@Builder
public class CollectionField extends SpecificRecordBase {
    private String key;
    private List<String> values;

    @Override
    public Schema getSchema() {
        return SchemaBuilder.record("CollectionField").fields()
                .requiredString("key")
                .name("values")
                .type(SchemaBuilder.array().items().stringType()).noDefault()
                .endRecord();
    }

    @Override
    public Object get(int field) {
        switch (field) {
            case 0:
                return key;
            case 1:
                return values;
            default:
                throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field, Object value) {
        switch (field) {
            case 0:
                this.key = (String) value;
                break;
            case 1:
                this.values = (List<String>) value;
                break;
            default:
                throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }
}
