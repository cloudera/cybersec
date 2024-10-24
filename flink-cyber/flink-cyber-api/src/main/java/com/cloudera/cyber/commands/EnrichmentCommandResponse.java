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

package com.cloudera.cyber.commands;

import static com.cloudera.cyber.AvroTypes.toListOf;

import com.cloudera.cyber.EnrichmentEntry;
import java.util.Collections;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@NoArgsConstructor
public class EnrichmentCommandResponse extends CommandResponse<EnrichmentEntry> {

    private static final Schema SCHEMA$ = SchemaBuilder
          .record(EnrichmentCommandResponse.class.getName())
          .namespace(EnrichmentCommandResponse.class.getPackage().getName())
          .fields()
          .requiredBoolean("success").requiredString("message")
          .name("content").type(Schema.createArray(EnrichmentEntry.SCHEMA$)).noDefault()
          .name("headers").type(Schema.createMap(Schema.create(Schema.Type.STRING))).withDefault(Collections.emptyMap())
          .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    List<EnrichmentEntry> putContent(Object value$) {
        return toListOf(EnrichmentEntry.class, value$);
    }
}
