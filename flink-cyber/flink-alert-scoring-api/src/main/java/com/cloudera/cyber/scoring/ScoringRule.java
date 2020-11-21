package com.cloudera.cyber.scoring;

import com.cloudera.cyber.AvroTypes;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.rules.BaseDynamicRule;
import com.cloudera.cyber.rules.RuleType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Data
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ScoringRule extends BaseDynamicRule implements SpecificRecord {
    @Builder.Default
    private UUID id = UUID.randomUUID();
    private boolean enabled = true;
    private int order;

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
            .name("id").type(AvroTypes.uuidType).noDefault()
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
            case 6: this.setId(UUID.fromString(value$.toString())); break;
            case 7: this.setEnabled((boolean) value$); break;
            default: throw new AvroRuntimeException("Bad index");
        }
    }
}
