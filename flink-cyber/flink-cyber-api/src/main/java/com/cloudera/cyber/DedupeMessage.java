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
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.Map;
import java.util.UUID;

import static com.cloudera.cyber.AvroTypes.utf8toStringMap;

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
            case 5: fields = utf8toStringMap(value$); break;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

}
