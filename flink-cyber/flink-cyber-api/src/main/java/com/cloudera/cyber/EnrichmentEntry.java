package com.cloudera.cyber;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.Map;

import static com.cloudera.cyber.AvroTypes.utf8toStringMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichmentEntry extends SpecificRecordBase implements SpecificRecord, Timestamped {

    private String key;
    private String type;
    private long ts;
    private Map<String,String> entries;

    public static final Schema SCHEMA$ = SchemaBuilder.record(EnrichmentEntry.class.getName()).namespace(EnrichmentEntry.class.getPackage().getName())
            .fields()
            .requiredString("key")
            .requiredString("type")
            .requiredLong("ts")
            .name("entries").type(Schema.createMap(SchemaBuilder.builder().stringType())).noDefault()
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    // Used by DatumWriter.  Applications should not call.
    public Object get(int field$) {
        switch (field$) {
            case 0: return key;
            case 1: return type;
            case 2: return ts;
            case 3: return entries;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value="unchecked")
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0: key = value$.toString(); break;
            case 1: type = value$.toString(); break;
            case 2: ts = (Long)value$; break;
            case 3: entries = utf8toStringMap(value$); break;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }
}
