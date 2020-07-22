package com.cloudera.cyber.scoring;

import com.cloudera.cyber.rules.DynamicRuleCommand;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder()
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ScoringRuleCommand extends DynamicRuleCommand<ScoringRule> {
}
