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

import com.cloudera.cyber.rules.DynamicRuleCommand;
import com.cloudera.cyber.rules.DynamicRuleCommandType;
import java.util.Arrays;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
//@TypeInfo(ScoringRuleCommandTypeFactory.class)
public class ScoringRuleCommand extends DynamicRuleCommand<ScoringRule> {

    public static Schema SCHEMA$ = SchemaBuilder.record(ScoringRuleCommand.class.getName())
          .namespace(ScoringRuleCommand.class.getPackage().getName())
          .fields()
          .requiredString("id")
          .name("type").type(Schema.createEnum(DynamicRuleCommandType.class.getName(), "",
                DynamicRuleCommandType.class.getPackage().getName(),
                Arrays.stream(DynamicRuleCommandType.values()).map(v -> v.name()).collect(Collectors.toList())))
          .noDefault()
          .requiredLong("ts")
          .optionalString("ruleId")
          .name("rule").type().optional().type(ScoringRule.SCHEMA$)
          .name("headers").type(Schema.createMap(Schema.create(Schema.Type.STRING))).noDefault()
          .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

}
