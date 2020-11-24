package com.cloudera.cyber.rules;

import com.cloudera.cyber.Message;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.avro.specific.SpecificRecordBase;

import java.time.Instant;
import java.util.Map;


@Getter
@Setter
@NoArgsConstructor
@ToString
public abstract class BaseDynamicRule extends SpecificRecordBase implements DynamicRule {
    private String name;
    private int order;

    private Instant tsStart;
    private Instant tsEnd;

    private RuleType type;

    private String ruleScript;

    protected BaseDynamicRule(BaseDynamicRuleBuilder<?, ?> b) {
        this.name = b.name;
        this.order = b.order;
        this.tsStart = b.tsStart;
        this.tsEnd = b.tsEnd;
        this.type = b.type;
        this.ruleScript = b.ruleScript;
    }

    @Override
    public Map<String, Object> apply(Message message) {
        return getType().engine(ruleScript).feed(message);
    }

    public static abstract class BaseDynamicRuleBuilder<C extends BaseDynamicRule, B extends BaseDynamicRuleBuilder<C, B>> {
        private String name;
        private int order;
        private Instant tsStart;
        private Instant tsEnd;
        private RuleType type;
        private String ruleScript;

        public B name(String name) {
            this.name = name;
            return self();
        }

        public B order(int order) {
            this.order = order;
            return self();
        }

        public B tsStart(Instant tsStart) {
            this.tsStart = tsStart;
            return self();
        }

        public B tsEnd(Instant tsEnd) {
            this.tsEnd = tsEnd;
            return self();
        }

        public B type(RuleType type) {
            this.type = type;
            return self();
        }

        public B ruleScript(String ruleScript) {
            this.ruleScript = ruleScript;
            return self();
        }

        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "BaseDynamicRule.BaseDynamicRuleBuilder(super=" + super.toString() + ", name=" + this.name + ", order=" + this.order + ", tsStart=" + this.tsStart + ", tsEnd=" + this.tsEnd + ", type=" + this.type + ", ruleScript=" + this.ruleScript + ")";
        }
    }
}
