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

import com.cloudera.cyber.Message;
import com.cloudera.cyber.rules.BaseDynamicRule;
import com.cloudera.cyber.rules.RuleType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.flink.api.common.typeinfo.TypeInfo;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@TypeInfo(ScoringRuleTypeFactory.class)
public class ScoringRule extends BaseDynamicRule<ScoringRule> {
    private String id = UUID.randomUUID().toString();
    private boolean enabled = true;

    public static final String RESULT_SCORE = "score";
    public static final String RESULT_REASON = "reason";


    protected ScoringRule(ScoringRuleBuilder<?, ?> b) {
        super(b);
        this.id = b.id;
        this.enabled = b.enabled;
    }

    public static ScoringRuleBuilder<?, ?> builder() {
        return new ScoringRuleBuilderImpl();
    }


    @Override
    public ScoringRule withId(String id) {
        return this.toBuilder().id(id).build();
    }

    @Override
    public ScoringRule withVersion(int version) {
        return this.toBuilder().version(version).build();
    }

    @Override
    public ScoringRule withEnabled(boolean enabled) {
        return this.toBuilder().enabled(enabled).build();
    }

    public Map<String, Object> apply(Message message) {
        return getType().engine(getRuleScript()).feed(message);
    }

    public static final Schema SCHEMA$ = SchemaBuilder
            .record(ScoringRule.class.getName())
            .namespace(ScoringRule.class.getPackage().getName())
            .fields()
            .requiredString("name")
            .requiredInt("order")
            .requiredLong("tsStart")
            .requiredLong("tsEnd")
            .name("type").type(Schema.createEnum(
                    RuleType.class.getName(),
                    "",
                    RuleType.class.getPackage().getName(),
                    Arrays.stream(RuleType.values()).map(Enum::name).collect(toList()))).noDefault()
            .requiredString("ruleScript")
            .requiredString("id")
            .requiredBoolean("enabled")
            .nullableInt("version", 0)
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0: return getName();
            case 1: return getOrder();
            case 2: return getTsStart().toEpochMilli();
            case 3: return getTsEnd().toEpochMilli();
            case 4: return getType();
            case 5: return getRuleScript();
            case 6: return id;
            case 7: return enabled;
            case 8: return getVersion();
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0: this.setName(value$.toString()); break;
            case 1: this.setOrder((int) value$); break;
            case 2: this.setTsStart(Instant.ofEpochMilli((long)value$)); break;
            case 3: this.setTsEnd(Instant.ofEpochMilli((long)value$)); break;
            case 4: this.setType(RuleType.valueOf(value$.toString())); break;
            case 5: this.setRuleScript(value$.toString()); break;
            case 6: this.setId(value$.toString()); break;
            case 7: this.setEnabled((boolean) value$); break;
            case 8 : this.setVersion((int) value$); break;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    public ScoringRuleBuilder<?, ?> toBuilder() {
        return new ScoringRuleBuilderImpl().$fillValuesFrom(this);
    }

    public static abstract class ScoringRuleBuilder<C extends ScoringRule, B extends ScoringRuleBuilder<C, B>> extends BaseDynamicRuleBuilder<C, B> {
        private String id;
        private boolean enabled;

        private static void $fillValuesFromInstanceIntoBuilder(ScoringRule instance, ScoringRuleBuilder<?, ?> b) {
            b.id(instance.id);
            b.enabled(instance.enabled);
        }

        public B id(String id) {
            this.id = id;
            return self();
        }

        public B enabled(boolean enabled) {
            this.enabled = enabled;
            return self();
        }

        protected B $fillValuesFrom(C instance) {
            super.$fillValuesFrom(instance);
            ScoringRuleBuilder.$fillValuesFromInstanceIntoBuilder(instance, this);
            return self();
        }

        protected abstract B self();

        public abstract C build();

        public String toString() {
            return "ScoringRule.ScoringRuleBuilder(super=" + super.toString() + ", id=" + this.id + ", enabled=" + this.enabled + ")";
        }
    }

    private static final class ScoringRuleBuilderImpl extends ScoringRuleBuilder<ScoringRule, ScoringRuleBuilderImpl> {
        private ScoringRuleBuilderImpl() {
        }

        protected ScoringRule.ScoringRuleBuilderImpl self() {
            return this;
        }

        public ScoringRule build() {
            return new ScoringRule(this);
        }
    }
}
