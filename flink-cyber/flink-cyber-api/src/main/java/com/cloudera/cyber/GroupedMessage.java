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

import java.util.List;
import java.util.UUID;

import static com.cloudera.cyber.AvroTypes.toListOf;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupedMessage extends SpecificRecordBase implements SpecificRecord, IdentifiedMessage, Timestamped {

    @Builder.Default
    private String id = UUID.randomUUID().toString();
    private List<Message> messages;

    public static final Schema SCHEMA$ = SchemaBuilder.record(GroupedMessage.class.getName()).namespace(GroupedMessage.class.getPackage().getName())
            .fields()
            .requiredString("id")
            .name("messages").type(
                    SchemaBuilder.array().items(Message.SCHEMA$)
            ).noDefault()
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0: return id;
            case 1: return messages;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value="unchecked")
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0: id = value$.toString(); break;
            case 1: messages = toListOf(Message.class, value$); break;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    @Override
    public long getTs() {
        return messages.get(0).getTs();
    }
}
