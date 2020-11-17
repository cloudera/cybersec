package com.cloudera.cyber;

import lombok.*;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.avro.util.Utf8;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toMap;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TypeInfo(MessageTypeFactory.class)
public class Message extends SpecificRecordBase implements SpecificRecord, IdentifiedMessage, Timestamped {
    @Builder.Default
    @NonNull private String id = UUID.randomUUID().toString();
    private long ts;
    @NonNull private SignedSourceKey originalSource;
    @Builder.Default
    @NonNull private String message = "";
    private Map<String, List<ThreatIntelligence>> threats;
    private Map<String, String> extensions;
    @NonNull private String source;
    private List<DataQualityMessage> dataQualityMessages;

    public static final Schema SCHEMA$ = SchemaBuilder.record(Message.class.getName()).namespace(Message.class.getPackage().getName())
            .fields()
            .requiredString("id")
            .requiredLong("ts")
            .name("originalSource").type(SignedSourceKey.SCHEMA$).noDefault()
            .requiredString("message")
            .name("threats").type().optional().type(SchemaBuilder.map().values(ThreatIntelligence.SCHEMA$))
            .name("extensions").type(Schema.createMap(Schema.create(Schema.Type.STRING))).noDefault()
            .requiredString("source")
            .name("dataQualityMessages").type().optional().type(Schema.createArray(DataQualityMessage.SCHEMA$))
            .endRecord();

    public static Schema getClassSchema() {
        return SCHEMA$;
    }

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0: return id;
            case 1: return ts;
            case 2: return originalSource;
            case 3: return message;
            case 4: return threats;
            case 5: return extensions;
            case 6: return source;
            case 7: return dataQualityMessages;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value="unchecked")
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
            case 0: id = value$.toString(); break;
            case 1: ts = (java.lang.Long)value$; break;
            case 2: originalSource = (com.cloudera.cyber.SignedSourceKey)value$; break;
            case 3:
                message = value$.toString();
                break;
            case 4: threats = (java.util.Map<java.lang.String,java.util.List<com.cloudera.cyber.ThreatIntelligence>>)value$; break;
            case 5: extensions = ((Map<Utf8,Utf8>)value$).entrySet().stream().collect(toMap(
                    k->k.getKey().toString(),
                    k->k.getValue().toString()
            )); break;
            case 6: source = value$.toString(); break;
            case 7: dataQualityMessages = (java.util.List<com.cloudera.cyber.DataQualityMessage>)value$; break;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }
}
