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

    public static class ScoringRuleCommandResultBuilder
          extends DynamicRuleCommandResultBuilder<ScoringRule, ScoringRuleCommandResult> {

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
