package com.cloudera.cyber.commands;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.Map;

import static com.cloudera.cyber.AvroTypes.utf8toStringMap;

@Data
@NoArgsConstructor
@SuperBuilder
public abstract class Command<T> extends SpecificRecordBase {
    private CommandType type;
    private T payload;
    private Map<String, String> headers;

    protected Command(CommandBuilder<T, ?, ?> b) {
        this.type = b.type;
        this.payload = b.payload;
        this.headers = b.headers;
    }

    @Override
    public abstract Schema getSchema();

    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0: return type;
            case 1: return payload;
            case 2: return headers;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value="unchecked")
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
            case 0: type = CommandType.valueOf(value$.toString()); break;
            case 1: payload = (T)value$; break;
            case 2: headers = utf8toStringMap(value$); break;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

}
