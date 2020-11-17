package com.cloudera.cyber.enrichment.stix.parsing;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Builder;
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
