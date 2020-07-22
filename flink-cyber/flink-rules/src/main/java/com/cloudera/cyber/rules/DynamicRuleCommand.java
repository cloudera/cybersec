package com.cloudera.cyber.rules;

import com.cloudera.cyber.Timestamped;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@ToString
public class DynamicRuleCommand<T extends DynamicRule> implements Timestamped {
    @NonNull protected UUID id;
    @NonNull protected DynamicRuleCommandType type;
    @NonNull protected Instant ts;
    protected UUID ruleId = null;
    protected T rule = null;

    public long getTs() {
        return ts.toEpochMilli();
    }
}
