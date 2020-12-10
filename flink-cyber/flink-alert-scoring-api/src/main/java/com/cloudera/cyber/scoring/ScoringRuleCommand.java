package com.cloudera.cyber.scoring;

import com.cloudera.cyber.rules.DynamicRuleCommand;
import com.cloudera.cyber.rules.DynamicRuleCommandType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.util.Arrays;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@TypeInfo(ScoringRuleCommandTypeFactory.class)
public class ScoringRuleCommand extends DynamicRuleCommand<ScoringRule> {

    public static Schema SCHEMA$ = SchemaBuilder.record(ScoringRuleCommand.class.getName())
            .namespace(ScoringRuleCommand.class.getPackage().getName())
            .fields()
            .requiredString("id")
            .name("type").type(Schema.createEnum(DynamicRuleCommandType.class.getName(), "", DynamicRuleCommandType.class.getPackage().getName(),
                    Arrays.stream(DynamicRuleCommandType.values()).map(v -> v.name()).collect(Collectors.toList()))).noDefault()
            .requiredLong("ts")
            .requiredString("ruleId")
            .name("rule").type(ScoringRule.SCHEMA$).noDefault()
            .name("headers").type(Schema.createMap(Schema.create(Schema.Type.STRING))).noDefault()
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

}
