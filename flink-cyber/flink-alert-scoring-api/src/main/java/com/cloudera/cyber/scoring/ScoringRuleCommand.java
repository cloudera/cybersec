package com.cloudera.cyber.scoring;

import com.cloudera.cyber.AvroTypes;
import com.cloudera.cyber.rules.DynamicRuleCommand;
import com.cloudera.cyber.rules.DynamicRuleCommandType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.util.Arrays;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@SuperBuilder
public class ScoringRuleCommand extends DynamicRuleCommand<ScoringRule> {

    private static Schema SCHEMA$ = SchemaBuilder.record(ScoringRuleCommand.class.getName())
            .namespace(ScoringRuleCommand.class.getPackage().getName())
            .fields()
            .name("id").type(AvroTypes.uuidType).noDefault()
            .name("type").type(Schema.createEnum(DynamicRuleCommandType.class.getName(), "", DynamicRuleCommandType.class.getPackage().getName(),
                    Arrays.stream(DynamicRuleCommandType.values()).map(v -> v.name()).collect(Collectors.toList()))).noDefault()
            .requiredLong("ts")
            .name("ruleId").type(AvroTypes.uuidType).noDefault()
            .name("rule").type(ScoringRule.SCHEMA$).noDefault()
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

}
