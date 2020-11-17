package com.cloudera.cyber;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.flink.api.common.typeinfo.TypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.util.Map;
import java.util.UUID;

import static com.cloudera.cyber.AvroTypes.utf8toStringMap;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeInfo(ThreatIntelligenceTypeFactory.class)
public class ThreatIntelligence extends SpecificRecordBase implements SpecificRecord, IdentifiedMessage, Timestamped {
    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private long ts;
    private String observable;
    private String observableType;
    private String stixReference;
    private Map<String, String> fields;

    public static final Schema SCHEMA$ = SchemaBuilder.record(ThreatIntelligence.class.getName()).namespace(ThreatIntelligence.class.getPackage().getName())
            .fields()
            .requiredString("id")
            .requiredLong("ts")
            .requiredString("observable")
            .requiredString("observableType")
            .optionalString("stixReference")
            .name("fields").type(Schema.createMap(SchemaBuilder.builder().stringType())).noDefault()
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0: return id;
            case 1: return ts;
            case 2: return observable;
            case 3: return observableType;
            case 4: return stixReference;
            case 5: return fields;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value="unchecked")
    @Override
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0: id = value$.toString(); break;
            case 1: ts = (Long)value$; break;
            case 2: observable = value$.toString(); break;
            case 3: observableType = value$.toString(); break;
            case 4: stixReference = value$.toString(); break;
            case 5: fields = utf8toStringMap(value$); break;
            default: throw new AvroRuntimeException("Bad index");
        }
    }
}