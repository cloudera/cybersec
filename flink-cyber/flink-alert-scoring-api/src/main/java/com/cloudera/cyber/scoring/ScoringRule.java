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
public class ScoringRule extends BaseDynamicRule {
    private String id = UUID.randomUUID().toString();
    private boolean enabled = true;
    private int order;

    public static final String RESULT_SCORE = "score";
    public static final String RESULT_REASON = "reason";

    protected ScoringRule(ScoringRuleBuilder<?, ?> b) {
        super(b);
        this.id = b.id;
        this.enabled = b.enabled;
        this.order = b.order;
    }

    public static ScoringRuleBuilder<?, ?> builder() {
        return new ScoringRuleBuilderImpl();
    }

    @Override
    public ScoringRule withId(String id) {
        return this.toBuilder().id(id).build();
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
                    Arrays.stream(RuleType.values()).map(s->s.name()).collect(toList()))).noDefault()
            .requiredString("ruleScript")
            .requiredString("id")
            .requiredBoolean("enabled")
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0: return getName();
            case 1: return order;
            case 2: return getTsStart();
            case 3: return getTsEnd();
            case 4: return getType();
            case 5: return getRuleScript();
            case 6: return id.toString();
            case 7: return enabled;
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
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    public ScoringRuleBuilder<?, ?> toBuilder() {
        return new ScoringRuleBuilderImpl().$fillValuesFrom(this);
    }

    public static abstract class ScoringRuleBuilder<C extends ScoringRule, B extends ScoringRuleBuilder<C, B>> extends BaseDynamicRuleBuilder<C, B> {
        private String id;
        private boolean enabled;
        private int order;

        private static void $fillValuesFromInstanceIntoBuilder(ScoringRule instance, ScoringRuleBuilder<?, ?> b) {
            b.id(instance.id);
            b.enabled(instance.enabled);
            b.order(instance.order);
        }

        public B id(String id) {
            this.id = id;
            return self();
        }

        public B enabled(boolean enabled) {
            this.enabled = enabled;
            return self();
        }

        public B order(int order) {
            this.order = order;
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
            return "ScoringRule.ScoringRuleBuilder(super=" + super.toString() + ", id=" + this.id + ", enabled=" + this.enabled + ", order=" + this.order + ")";
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
