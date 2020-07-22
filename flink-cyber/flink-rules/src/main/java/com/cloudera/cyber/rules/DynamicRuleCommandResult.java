package com.cloudera.cyber.rules;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Builder
@Data
public class DynamicRuleCommandResult<R extends DynamicRule> {
    UUID cmdId;
    boolean success;
    R rule;
}
