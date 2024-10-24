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

import static com.cloudera.cyber.AvroTypes.toListOf;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupedMessage extends SpecificRecordBase implements SpecificRecord, IdentifiedMessage, Timestamped {

    public static final Schema SCHEMA$ =
          SchemaBuilder.record(GroupedMessage.class.getName()).namespace(GroupedMessage.class.getPackage().getName())
                .fields()
                .requiredString("id")
                .name("messages").type(
                      SchemaBuilder.array().items(Message.SCHEMA$)
                ).noDefault()
                .endRecord();
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private List<Message> messages;

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0:
                return id;
            case 1:
                return messages;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value = "unchecked")
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0:
                id = value$.toString();
                break;
            case 1:
                messages = toListOf(Message.class, value$);
                break;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }

    @Override
    public long getTs() {
        return messages.get(0).getTs();
    }
}
