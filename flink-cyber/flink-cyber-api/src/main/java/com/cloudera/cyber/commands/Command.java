package com.cloudera.cyber.commands;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

@Data
@NoArgsConstructor
public abstract class Command<T> extends SpecificRecordBase {
    private CommandType type;
    private T payload;

    protected Command(CommandBuilder<T, ?, ?> b) {
        this.type = b.type;
        this.payload = b.payload;
    }

    @Override
    public abstract Schema getSchema();

    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0: return type;
            case 1: return payload;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value="unchecked")
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
            case 0: type = CommandType.valueOf(value$.toString()); break;
            case 1: payload = (T)value$; break;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    public static abstract class CommandBuilder<T, C extends Command<T>, B extends CommandBuilder<T, C, B>> {
        private CommandType type;
        private T payload;

        public B type(CommandType type) {
            this.type = type;
            return self();
        }

        public B payload(T payload) {
            this.payload = payload;
            return self();
        }

        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "Command.CommandBuilder(super=" + super.toString() + ", type=" + this.type + ", payload=" + this.payload + ")";
        }
    }
}
