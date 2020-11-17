package com.cloudera.cyber.enrichment.stix.parsing;

import com.cloudera.cyber.ThreatIntelligence;
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
