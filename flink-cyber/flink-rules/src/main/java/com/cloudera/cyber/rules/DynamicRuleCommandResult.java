package com.cloudera.cyber.rules;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

@Data
@NoArgsConstructor
public abstract class DynamicRuleCommandResult<R extends DynamicRule> extends SpecificRecordBase {
    protected String cmdId;
    protected boolean success;
    protected R rule;
    protected Integer parallelSubtaskNumber;

    protected DynamicRuleCommandResult(String cmdId, boolean success, R rule, int numberOfParallelSubtask) {
        this.cmdId = cmdId;
        this.success = success;
        this.rule = rule;
        this.parallelSubtaskNumber = numberOfParallelSubtask;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0:
                return cmdId;
            case 1:
                return success;
            case 2:
                return rule;
            case 3:
                return parallelSubtaskNumber;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0:
                cmdId = value$.toString();
                break;
            case 1:
                success = (boolean) value$;
                break;
            case 2:
                rule = (R) value$;
                break;
            case 3:
                parallelSubtaskNumber = (Integer) value$;
                break;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }

    public abstract Schema getSchema();

    public abstract static class DynamicRuleCommandResultBuilder<R extends DynamicRule, RESULT extends DynamicRuleCommandResult<R>> {
        protected @NonNull String cmdId;
        protected boolean success;
        protected R rule;
        protected int parallelSubtaskNumber;

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

        public DynamicRuleCommandResultBuilder<R, RESULT> subtaskNumber(int numberOfParallelSubtask) {
            this.parallelSubtaskNumber = numberOfParallelSubtask;
            return self();
        }

        public abstract RESULT build();

        public String toString() {
            return "DynamicRuleCommandResult.DynamicRuleCommandResultBuilder(super=" + super.toString() + ", cmdId=" + this.cmdId + ", success=" + this.success + ", rule=" + this.rule + ")";
        }
    }
}
