package com.cloudera.cyber.scoring;

import com.cloudera.cyber.rules.DynamicRuleCommandResult;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;


@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class ScoringRuleCommandResult extends DynamicRuleCommandResult<ScoringRule> {

    private ScoringRuleCommandResult(String cmdId, boolean success, ScoringRule rule, int parallelSubtaskNumber) {
        super(cmdId, success, rule, parallelSubtaskNumber);
    }

    public static final Schema SCHEMA$ = SchemaBuilder.record(ScoringRuleCommandResult.class.getName())
            .namespace(ScoringRuleCommandResult.class.getPackage().getName())
            .fields()
            .requiredString("id")
            .requiredBoolean("result")
            .name("rule").type().optional().type(ScoringRule.SCHEMA$)
            .optionalInt("parallelSubtaskNumber")
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    public static ScoringRuleCommandResultBuilder builder() {
        return new ScoringRuleCommandResultBuilder();
    }

    public static class ScoringRuleCommandResultBuilder extends DynamicRuleCommandResultBuilder<ScoringRule, ScoringRuleCommandResult> {

        @Override
        protected ScoringRuleCommandResultBuilder self() {
            return this;
        }

        @Override
        public ScoringRuleCommandResult build() {
            return new ScoringRuleCommandResult(cmdId, success, rule, parallelSubtaskNumber);
        }
    }
}
