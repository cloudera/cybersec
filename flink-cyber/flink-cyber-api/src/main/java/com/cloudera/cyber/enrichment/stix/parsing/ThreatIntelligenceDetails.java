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

package com.cloudera.cyber.enrichment.stix.parsing;

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
public class ThreatIntelligenceDetails extends SpecificRecordBase implements SpecificRecord {

    private String id;
    private String stixSource;

    public static final Schema SCHEMA$ = SchemaBuilder
            .record(ThreatIntelligenceDetails.class.getName())
            .namespace(ThreatIntelligenceDetails.class.getPackage().getName())
            .fields()
            .requiredString("id")
            .requiredString("stixSource")
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0: return id;
            case 1: return stixSource;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value="unchecked")
    @Override
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
            case 0: id = value$.toString(); break;
            case 1: stixSource = value$.toString(); break;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

}
