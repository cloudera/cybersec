package com.cloudera.cyber.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import java.nio.ByteBuffer;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageToParse extends SpecificRecordBase implements SpecificRecord {
    private byte[] originalBytes;
    private String topic;
    private int partition;
    private long offset;
    private byte[] key;

    public static final Schema SCHEMA$ = SchemaBuilder.record(MessageToParse.class.getName()).namespace(MessageToParse.class.getPackage().getName())
            .fields()
            .requiredBytes("originalBytes")
            .requiredString("topic")
            .requiredInt("partition")
            .requiredLong("offset")
            .optionalBytes("key")
            .endRecord();

    public static Schema getClassSchema() { return SCHEMA$; }

    @Override
    public Schema getSchema() { return SCHEMA$; }


    @Override
    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0: return ByteBuffer.wrap(originalBytes);
            case 1: return topic;
            case 2: return partition;
            case 3: return offset;
            case 4: return (key != null) ? ByteBuffer.wrap(key) : null;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
            case 0: originalBytes = (value$ instanceof byte[]) ? (byte[])value$: ((ByteBuffer) value$).array(); break;
            case 1: topic = value$.toString(); break;
            case 2: partition = (int)value$; break;
            case 3: offset = (long)value$; break;
            case 4:  key = (value$ == null) ? null : ((value$ instanceof byte[]) ? (byte[])value$: ((ByteBuffer) value$).array()); break;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

}
