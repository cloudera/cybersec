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

package com.cloudera.cyber.indexing;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectionField extends SpecificRecordBase implements SpecificRecord {
    private String key;
    private List<String> values;

    private static final Schema SCHEMA$ = SchemaBuilder
          .record(CollectionField.class.getName())
          .namespace(CollectionField.class.getPackage().getName())
          .fields()
          .requiredString("key")
          .name("values")
          .type(SchemaBuilder.array().items().stringType()).noDefault()
          .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field) {
        switch (field) {
            case 0:
                return key;
            case 1:
                return values;
            default:
                throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field, Object value) {
        switch (field) {
            case 0:
                this.key = value.toString();
                break;
            case 1:
                this.values = (List<String>) value;
                break;
            default:
                throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }
}
