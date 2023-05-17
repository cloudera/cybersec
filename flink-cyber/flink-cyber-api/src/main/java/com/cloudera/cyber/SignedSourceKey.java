/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

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
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.types.Row;

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

    public static final TypeInformation<Row> FLINK_TYPE_INFO = Types.ROW_NAMED(
            new String[]{"topic", "partition", "offset", "signature"},
            Types.STRING, Types.INT, Types.LONG, Types.PRIMITIVE_ARRAY(Types.BYTE));


    public Row toRow() {
        return Row.of(topic, partition, offset, signature);
    }

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
