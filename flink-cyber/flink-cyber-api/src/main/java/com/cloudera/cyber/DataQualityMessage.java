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
import org.apache.flink.api.common.typeinfo.TypeInfo;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TypeInfo(DataQualityMessageTypeInfo.class)
public class DataQualityMessage extends SpecificRecordBase implements SpecificRecord {

    private String level;
    private String feature;
    private String field;
    private String message;

    public static final Schema SCHEMA$ = SchemaBuilder
            .record(DataQualityMessage.class.getName())
            .namespace(DataQualityMessage.class.getPackage().getName())
            .fields()
            .requiredString("level")
            .requiredString("feature")
            .requiredString("field")
            .requiredString("message")
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    public Object get(int field$) {
        switch (field$) {
            case 0: return level;
            case 1: return feature;
            case 2: return field;
            case 3: return message;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value="unchecked")
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0: level = value$.toString(); break;
            case 1: feature = value$.toString(); break;
            case 2: field = value$.toString(); break;
            case 3: message = value$.toString(); break;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

}
