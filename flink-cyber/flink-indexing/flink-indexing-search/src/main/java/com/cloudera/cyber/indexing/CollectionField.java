package com.cloudera.cyber.indexing;

import lombok.*;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.List;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionField extends SpecificRecordBase implements SpecificRecord {
    private String key;
    private List<String> values;

    private static final Schema SCHEMA$ = SchemaBuilder
            .record(CollectionField.class.getName())
            .namespace(CollectionField.class.getPackage().getName())
            .fields()
            .requiredString("key")
            .name("values")
            .type(SchemaBuilder.array().items().stringType()).noDefault()
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
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
                this.key = value.toString();
                break;
            case 1:
                this.values = (List<String>) value;
                break;
            default:
                throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }
}
