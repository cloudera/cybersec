package com.cloudera.cyber.commands;

import com.cloudera.cyber.EnrichmentEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.util.Collections;
import java.util.List;

import static com.cloudera.cyber.AvroTypes.toListOf;

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
