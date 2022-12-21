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

package com.cloudera.cyber.enrichment.threatq;

import com.cloudera.cyber.EnrichmentEntry;
import com.cyber.jackson.annotation.JsonFormat;
import com.hortonworks.registries.schemaregistry.serdes.avro.exceptions.AvroException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecordBase;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloudera.cyber.AvroTypes.toListOf;
import static com.cloudera.cyber.AvroTypes.utf8toStringMap;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ThreatQEntry extends SpecificRecordBase {
    private String indicator;
    private List<String> tq_sources;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date tq_created_at;
    private Float tq_score;
    private String tq_type;
    private String tq_saved_search;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date tq_updated_at;
    @JsonFormat
            (shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date tq_touched_at;

    private Long tq_id;
    private Map<String, String> tq_attributes;
    private String tq_status;
    private String tq_url;
    private List<String> tq_tags;

    public static EnrichmentEntry toEnrichmentEntry(ThreatQEntry threatQEntry) {
        return EnrichmentEntry.builder()
                .ts(threatQEntry.tq_updated_at.getTime())
                .key(threatQEntry.getIndicator())
                .type("threatq")
                .entries(new HashMap<String, String>() {{
                    putAll(threatQEntry.getTq_attributes());
                    put("tq_id", threatQEntry.getTq_id().toString());
                    put("tq_status", threatQEntry.getTq_status());
                    put("tq_url", threatQEntry.getTq_url());
                    if (threatQEntry.getTq_tags() != null && threatQEntry.getTq_tags().size() > 0) {
                        put("tq_tags", threatQEntry.getTq_tags().toString());
                    }
                    put("tq_type", threatQEntry.getTq_type());
                    put("tq_saved_search", threatQEntry.getTq_saved_search());
                    if (threatQEntry.getTq_sources() != null && threatQEntry.getTq_sources().size() > 0) {
                        put("tq_sources", threatQEntry.getTq_sources().toString());
                    }
                    put("tq_score", threatQEntry.getTq_score().toString());
                }})
                .build();
    }

    public static Schema SCHEMA$ = SchemaBuilder
            .record(ThreatQEntry.class.getName())
            .namespace(ThreatQEntry.class.getPackage().getName())
            .fields()
            .requiredString("indicator")
            .name("tq_sources").type(SchemaBuilder.array().items(SchemaBuilder.builder().stringType())).noDefault()
            .requiredLong("tq_created_at")
            .requiredFloat("tq_score")
            .requiredString("tq_type")
            .requiredString("tq_saved_search")
            .requiredLong("tq_updated_at")
            .requiredLong("tq_touched_at")
            .requiredLong("tq_id")
            .name("tq_attributes").type(SchemaBuilder.map().values(SchemaBuilder.builder().stringType())).noDefault()
            .requiredString("tq_status")
            .requiredString("tq_url")
            .name("tq_tags").type(SchemaBuilder.array().items(SchemaBuilder.builder().stringType())).noDefault()
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field) {
        switch(field) {
            case 0: return indicator;
            case 1: return tq_sources;
            case 2: return tq_created_at;
            case 3: return tq_score;
            case 4: return tq_type;
            case 5: return tq_saved_search;
            case 6: return tq_updated_at;
            case 7: return tq_touched_at;
            case 8: return tq_id;
            case 9: return tq_attributes;
            case 10: return tq_status;
            case 11: return tq_url;
            case 12: return tq_tags;
            default: throw new AvroException("Bad Index");
        }
    }

    @Override
    public void put(int field, Object value) {
        switch(field) {
            case 0: this.indicator = value.toString(); break;
            case 1: this.tq_sources = toListOf(String.class, value); break;
            case 2: this.tq_created_at = value instanceof Date ? (Date) value : new Date((long)value); break;
            case 3: this.tq_score = (Float) value; break;
            case 4: this.tq_type = value.toString(); break;
            case 5: this.tq_saved_search = value.toString(); break;
            case 6: this.tq_updated_at = value instanceof Date ? (Date) value : new Date((long)value); break;
            case 7: this.tq_touched_at = value instanceof Date ? (Date) value : new Date((long)value); break;
            case 8: this.tq_id = (Long) value; break;
            case 9: this.tq_attributes = utf8toStringMap(value); break;
            case 10: this.tq_status = value.toString(); break;
            case 11: this.tq_url = value.toString(); break;
            case 12: this.tq_tags = toListOf(String.class, value); break;
            default: throw new AvroException("Bad Index");
        }
    }
}
