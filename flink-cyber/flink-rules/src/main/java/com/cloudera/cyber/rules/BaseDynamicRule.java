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

package com.cloudera.cyber.rules;

import com.cloudera.cyber.Message;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.avro.specific.SpecificRecordBase;


@Getter
@Setter
@NoArgsConstructor
@ToString
public abstract class BaseDynamicRule<R> extends SpecificRecordBase implements DynamicRule<R> {
    private String name;
    private int order;

    private Instant tsStart;
    private Instant tsEnd;

    private RuleType type;

    private String ruleScript;
    private int version;

    protected BaseDynamicRule(BaseDynamicRuleBuilder<?, ?> b) {
        this.name = b.name;
        this.order = b.order;
        this.tsStart = b.tsStart;
        this.tsEnd = b.tsEnd;
        this.type = b.type;
        this.ruleScript = b.ruleScript;
        this.version = b.version;
    }

    @Override
    public boolean isValid() {
        return getType().engine(this.getRuleScript()).validate();
    }

    @Override
    public Map<String, Object> apply(Message message) {
        return getType().engine(ruleScript).feed(message);
    }

    public abstract static class BaseDynamicRuleBuilder<C extends BaseDynamicRule, B extends BaseDynamicRuleBuilder<C, B>> {
        private String name;
        private int order;
        private Instant tsStart;
        private Instant tsEnd;
        private RuleType type;
        private String ruleScript;
        private int version;

        private static void $fillValuesFromInstanceIntoBuilder(BaseDynamicRule instance,
                                                               BaseDynamicRuleBuilder<?, ?> b) {
            b.name(instance.name);
            b.order(instance.order);
            b.tsStart(instance.tsStart);
            b.tsEnd(instance.tsEnd);
            b.type(instance.type);
            b.ruleScript(instance.ruleScript);
            b.version(instance.version);
        }

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

        public B version(int version) {
            this.version = version;
            return self();
        }

        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "BaseDynamicRule.BaseDynamicRuleBuilder(super=" + super.toString() + ", name=" + this.name
                   + ", order=" + this.order + ", tsStart=" + this.tsStart + ", tsEnd=" + this.tsEnd + ", type="
                   + this.type + ", ruleScript=" + this.ruleScript + ", version=" + this.version + ")";
        }

        protected B $fillValuesFrom(C instance) {
            BaseDynamicRuleBuilder.$fillValuesFromInstanceIntoBuilder(instance, this);
            return self();
        }
    }
}
