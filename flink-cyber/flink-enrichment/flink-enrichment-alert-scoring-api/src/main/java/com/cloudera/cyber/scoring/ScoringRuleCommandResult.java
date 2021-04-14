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

    private ScoringRuleCommandResult(String cmdId, boolean success, ScoringRule rule) {
        super(cmdId, success, rule);
    }

    public static Schema SCHEMA$ = SchemaBuilder.record(ScoringRuleCommandResult.class.getName())
            .namespace(ScoringRuleCommandResult.class.getPackage().getName())
            .fields()
            .requiredString("id")
            .requiredBoolean("result")
            .name("rule").type().optional().type(ScoringRule.SCHEMA$)
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
        protected DynamicRuleCommandResultBuilder self() {
            return this;
        }

        @Override
        public ScoringRuleCommandResult build() {
            return new ScoringRuleCommandResult(cmdId, success, rule);
        }
    }

}
