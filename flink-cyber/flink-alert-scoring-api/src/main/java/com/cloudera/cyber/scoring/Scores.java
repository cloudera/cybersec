package com.cloudera.cyber.scoring;

import com.cloudera.cyber.AvroTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scores extends SpecificRecordBase implements SpecificRecord {
    @Builder.Default
    private UUID ruleId = UUID.randomUUID();
    private Double score;
    private String reason;

    public static final Schema SCHEMA$ = SchemaBuilder.record(Scores.class.getName())
            .namespace(Scores.class.getPackage().getName())
            .fields()
            .name("ruleId").type(AvroTypes.uuidType).noDefault()
            .requiredDouble("score")
            .requiredString("reason")
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0: return ruleId;
            case 1: return score;
            case 2: return reason;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0: ruleId = (UUID) value$; break;
            case 1: score = (Double)value$; break;
            case 2: reason = value$.toString(); break;
            default: throw new AvroRuntimeException("Bad Index");
        }
    }
}
