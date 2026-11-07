package com.cloudera.cyber.indexing;

import com.cloudera.cyber.Timestamped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

/**
 * Create this record when a row that is read from a table fails to convert to a Message because it
 * is missing fields or has values that the MapRowToMessage.class is not able to convert.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class MessageQueryConversionError extends SpecificRecordBase implements SpecificRecord, Timestamped {
    private long ts;
    private String conversionError;
    private String rowText;

    private static final Schema SCHEMA$ = SchemaBuilder
            .record(MessageQueryConversionError.class.getName())
            .namespace(MessageQueryConversionError.class.getPackage().getName())
            .fields()
            .name("ts").type(LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG))).noDefault()
            .requiredString("conversionError").requiredString("rowText")
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field$) {
        return switch (field$) {
            case 0 -> ts;
            case 1 -> conversionError;
            case 2 -> rowText;
            default -> throw new org.apache.avro.AvroRuntimeException("Bad index");
        };
    }

    @Override
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0: ts = (java.lang.Long)value$; break;
            case 1: conversionError = value$.toString(); break;
            case 2: rowText = value$.toString(); break;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }
}
