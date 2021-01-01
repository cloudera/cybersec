package com.cloudera.cyber.rules;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

@Data
@NoArgsConstructor
public abstract class DynamicRuleCommandResult<R extends DynamicRule> extends SpecificRecordBase {
    String cmdId;
    boolean success;
    R rule;

    protected DynamicRuleCommandResult(String cmdId, boolean success, R rule) {
        this.cmdId = cmdId;
        this.success = success;
        this.rule = rule;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0: return cmdId;
            case 1: return success;
            case 2: return rule;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0: cmdId = value$.toString(); break;
            case 1: success = (boolean)value$; break;
            case 2: rule = (R)value$; break;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    public abstract Schema getSchema();

    public static abstract class DynamicRuleCommandResultBuilder<R extends DynamicRule, RESULT extends DynamicRuleCommandResult<R>> {
        protected @NonNull String cmdId;
        protected boolean success;
        protected R rule;

        public DynamicRuleCommandResultBuilder<R, RESULT> cmdId(@NonNull String cmdId) {
            this.cmdId = cmdId;
            return self();
        }

        public DynamicRuleCommandResultBuilder<R, RESULT> success(boolean success) {
            this.success = success;
            return self();
        }

        public DynamicRuleCommandResultBuilder<R, RESULT> rule(R rule) {
            this.rule = rule;
            return self();
        }

        protected DynamicRuleCommandResultBuilder<R, RESULT> self() {
            return this;
        }

        public abstract RESULT build();

        public String toString() {
            return "DynamicRuleCommandResult.DynamicRuleCommandResultBuilder(super=" + super.toString() + ", cmdId=" + this.cmdId + ", success=" + this.success + ", rule=" + this.rule + ")";
        }
    }

}
