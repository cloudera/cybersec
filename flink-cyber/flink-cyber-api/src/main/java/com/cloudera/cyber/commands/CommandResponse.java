package com.cloudera.cyber.commands;

import com.cloudera.cyber.flink.HasHeaders;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.List;
import java.util.Map;

import static com.cloudera.cyber.AvroTypes.utf8toStringMap;

@Data
@NoArgsConstructor
public abstract class CommandResponse<T> extends SpecificRecordBase implements HasHeaders {
    private boolean success;
    private String message;
    private List<T> content;
    private Map<String,String> headers;

    protected CommandResponse(CommandResponseBuilder<T, ?, ?> b) {
        this.success = b.success;
        this.message = b.message;
        this.content = b.content;
        this.headers = b.headers;
    }

    @Override
    public abstract Schema getSchema();

    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0: return success;
            case 1: return message;
            case 2: return content;
            case 3: return headers;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value="unchecked")
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
            case 0: success = (boolean) value$; break;
            case 1: message = value$ == null ? "" : value$.toString(); break;
            case 2: content = putContent(value$); break;
            case 3: headers = utf8toStringMap(value$); break;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    abstract List<T> putContent(Object value$);

    public static abstract class CommandResponseBuilder<T, C extends CommandResponse<T>, B extends CommandResponseBuilder<T, C, B>> {
        private boolean success;
        private String message;
        private List<T> content;
        private Map<String, String> headers;

        public B success(boolean success) {
            this.success = success;
            return self();
        }

        public B message(String message) {
            this.message = message;
            return self();
        }

        public B content(List<T> content) {
            this.content = content;
            return self();
        }

        public B headers(Map<String, String> headers) {
            this.headers = headers;
            return self();
        }

        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "CommandResponse.CommandResponseBuilder(super=" + super.toString() + ", success=" + this.success + ", message=" + this.message + ", content=" + this.content + ", headers=" + this.headers + ")";
        }
    }
}
