package com.cloudera.cyber.scoring;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.rules.BaseDynamicRule;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.Map;
import java.util.UUID;

@Data
@SuperBuilder(toBuilder = true)
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ScoringRule extends BaseDynamicRule {
    UUID id;
    boolean enabled = true;
    int order;

    public static final String RESULT_SCORE = "score";
    public static final String RESULT_REASON = "reason";

    @Override
    public ScoringRule withId(UUID uuid) {
        return this.toBuilder().id(uuid).build();
    }

    @Override
    public ScoringRule withEnabled(boolean enabled) {
        return this.toBuilder().enabled(enabled).build();
    }

    public Map<String, Object> apply(Message message) {
        return getType().engine(getRuleScript()).feed(message);
    }


}
