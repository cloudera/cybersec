package com.cloudera.cyber.rules;

import com.cloudera.cyber.Message;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@ToString
public abstract class BaseDynamicRule implements DynamicRule {
    private String name;
    private int order;

    private Instant tsStart;
    private Instant tsEnd;

    private RuleType type;

    private String ruleScript;

    @Override
    public Map<String, Object> apply(Message message) {
        return getType().engine(ruleScript).feed(message);
    }
}
