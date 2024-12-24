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

package com.cloudera.cyber.rules;

import static com.cloudera.cyber.AvroTypes.utf8toStringMap;

import com.cloudera.cyber.Timestamped;
import com.cloudera.cyber.flink.HasHeaders;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.specific.SpecificRecordBase;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public abstract class DynamicRuleCommand<T extends DynamicRule> extends SpecificRecordBase
      implements Timestamped, HasHeaders {
    @NonNull
    protected String id;
    @NonNull
    protected DynamicRuleCommandType type;
    @NonNull
    protected long ts;
    protected String ruleId = null;
    protected T rule = null;
    protected Map<String, String> headers;

    protected DynamicRuleCommand(DynamicRuleCommandBuilder<T, ?, ?> b) {
        this.id = b.id;
        this.type = b.type;
        this.ts = b.ts;
        this.ruleId = b.ruleId;
        this.rule = b.rule;
        this.headers = b.headers;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0:
                return id;
            case 1:
                return type;
            case 2:
                return ts;
            case 3:
                return ruleId;
            case 4:
                return rule;
            case 5:
                return headers;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0:
                id = value$.toString();
                break;
            case 1:
                type = DynamicRuleCommandType.valueOf(value$.toString());
                break;
            case 2:
                ts = (long) value$;
                break;
            case 3:
                ruleId = (value$ != null) ? (value$.toString()) : null;
                break;
            case 4:
                rule = (T) value$;
                break;
            case 5:
                headers = utf8toStringMap(value$);
                break;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }

    public abstract static class DynamicRuleCommandBuilder<
          T extends DynamicRule,
          C extends DynamicRuleCommand<T>,
          B extends DynamicRuleCommandBuilder<T, C, B>> {
        private @NonNull String id;
        private @NonNull DynamicRuleCommandType type;
        private @NonNull long ts;
        private String ruleId;
        private T rule;
        private Map<String, String> headers;

        public B id(@NonNull String id) {
            this.id = id;
            return self();
        }

        public B type(@NonNull DynamicRuleCommandType type) {
            this.type = type;
            return self();
        }

        public B ts(@NonNull long ts) {
            this.ts = ts;
            return self();
        }

        public B ruleId(String ruleId) {
            this.ruleId = ruleId;
            return self();
        }

        public B rule(T rule) {
            this.rule = rule;
            return self();
        }

        public B headers(Map<String, String> headers) {
            this.headers = headers;
            return self();
        }

        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "DynamicRuleCommand.DynamicRuleCommandBuilder(super=" + super.toString() + ", id=" + this.id
                   + ", type=" + this.type + ", ts=" + this.ts + ", ruleId=" + this.ruleId + ", rule=" + this.rule
                   + ", headers=" + this.headers + ")";
        }
    }
}
