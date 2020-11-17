package com.cloudera.cyber;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder=true)
@NoArgsConstructor
@AllArgsConstructor
public class DedupeMessage extends SpecificRecordBase implements SpecificRecord, IdentifiedMessage, Timestamped {

    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private long ts;
    private long startTs;
    private long count;
    private boolean late;
    private Map<String,String> fields;

    static Schema schema = SchemaBuilder.record(DedupeMessage.class.getName()).namespace(DedupeMessage.class.getPackage().getName())
            .fields().requiredString("id")
            .requiredLong("ts")
            .requiredLong("startTs")
            .requiredLong("count")
            .requiredBoolean("late")
            .name("fields").type(Schema.createMap(Schema.create(Schema.Type.STRING))).noDefault()
            .endRecord();

    @Override
    public Schema getSchema() {
        return schema;
    }

    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0: return id;
            case 1: return ts;
            case 2: return startTs;
            case 3: return count;
            case 4: return late;
            case 5: return fields;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value="unchecked")
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
            case 0: id = value$.toString(); break;
            case 1: ts = (java.lang.Long)value$; break;
            case 2: startTs = (java.lang.Long)value$; break;
            case 3: count = (java.lang.Long)value$; break;
            case 4: late = (java.lang.Boolean)value$; break;
            case 5: fields = (java.util.Map<java.lang.String,java.lang.String>)value$; break;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

}
