package com.cloudera.cyber.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageToParse extends SpecificRecordBase implements SpecificRecord {
    private String originalSource;
    private String topic;
    private int partition;
    private long offset;

    public static final Schema SCHEMA$ = SchemaBuilder.record(MessageToParse.class.getName()).namespace(MessageToParse.class.getPackage().getName())
            .fields()
            .requiredString("originalSource")
            .requiredString("topic")
            .requiredInt("partition")
            .requiredLong("offest")
            .endRecord();

    public static Schema getClassSchema() { return SCHEMA$; }

    @Override
    public Schema getSchema() { return SCHEMA$; }


    @Override
    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0: return originalSource;
            case 1: return topic;
            case 2: return partition;
            case 3: return offset;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    @SuppressWarnings(value="unchecked")
    @Override
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
            case 0: originalSource = value$.toString(); break;
            case 1: topic = value$.toString(); break;
            case 2: partition = (int)value$; break;
            case 3: offset = (long)value$; break;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

}
