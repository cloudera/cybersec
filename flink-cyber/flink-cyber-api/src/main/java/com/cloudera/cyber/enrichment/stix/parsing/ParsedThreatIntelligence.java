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

import com.cloudera.cyber.ThreatIntelligence;
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
public class ParsedThreatIntelligence extends SpecificRecordBase implements SpecificRecord {
    private String source;
    private ThreatIntelligence threatIntelligence;

    public static final Schema SCHEMA$ = SchemaBuilder.record(ParsedThreatIntelligence.class.getName()).namespace(ParsedThreatIntelligence.class.getPackage().getName())
            .fields()
            .requiredString("source")
            .name("threatIntelligence").type(ThreatIntelligence.SCHEMA$).noDefault()
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0:
                return source;
            case 1:
                return threatIntelligence;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value = "unchecked")
    @Override
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0:
                source = (String) value$;
                break;
            case 1:
                threatIntelligence = (ThreatIntelligence) value$;
                break;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }

}
