/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.commands;

import static com.cloudera.cyber.AvroTypes.utf8toStringMap;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

@Data
@NoArgsConstructor
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
            case 0:
                return type;
            case 1:
                return payload;
            case 2:
                return headers;
            default:
                throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value = "unchecked")
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
            case 0:
                type = CommandType.valueOf(value$.toString());
                break;
            case 1:
                payload = (T) value$;
                break;
            case 2:
                headers = utf8toStringMap(value$);
                break;
            default:
                throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    public abstract static class CommandBuilder<T, C extends Command<T>, B extends CommandBuilder<T, C, B>> {
        private CommandType type;
        private T payload;
        private Map<String, String> headers;

        public B type(CommandType type) {
            this.type = type;
            return self();
        }

        public B payload(T payload) {
            this.payload = payload;
            return self();
        }

        public B headers(Map<String, String> headers) {
            this.headers = headers;
            return self();
        }

        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "Command.CommandBuilder(super=" + super.toString() + ", type=" + this.type + ", payload="
                   + this.payload + ", headers=" + this.headers + ")";
        }
    }
}
