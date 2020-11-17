package com.cloudera.cyber;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;

import lombok.NoArgsConstructor;
import org.apache.avro.*;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.nio.ByteBuffer;

@Data
@Builder
//@TypeInfo(SignedSourceKeyTypeFactory.class)
@NoArgsConstructor
@AllArgsConstructor
public class SignedSourceKey extends SpecificRecordBase implements SpecificRecord {
    private String topic;
    private int partition;
    private long offset;
    private byte[] signature;
    public static final Schema SCHEMA$ = SchemaBuilder
            .record(SignedSourceKey.class.getName())
            .namespace(SignedSourceKey.class.getPackage().getName())
            .fields()
            .requiredString("topic")
            .requiredInt("partition")
            .requiredLong("offset")
            .name("signature").type().bytesBuilder().endBytes().noDefault()
            .endRecord();

//    static public class SignedSourceKeyBuilder {
//        public SignedSourceKeyBuilder signature(byte[] bytes) {
//            this.signature = ByteBuffer.wrap(bytes);
//            return this;
//        }
//    }

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field) {
        switch(field) {
            case 0: return topic;
            case 1: return partition;
            case 2: return offset;
            case 3: return ByteBuffer.wrap(signature);
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field, Object value) {
        switch(field) {
            case 0: this.topic = value.toString(); break;
            case 1: this.partition = (int) value; break;
            case 2: this.offset = (long) value; break;
            case 3: this.signature = (value instanceof byte[]) ? (byte[])value: ((ByteBuffer) value).array(); break;
            default: throw new AvroRuntimeException("Bad index");
        }
    }
}
