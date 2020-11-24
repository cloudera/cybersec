package com.cloudera.cyber.rules;

import com.cloudera.cyber.Timestamped;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@SuperBuilder
public abstract class DynamicRuleCommand<T extends DynamicRule> extends SpecificRecordBase implements Timestamped {
    @NonNull protected UUID id;
    @NonNull protected DynamicRuleCommandType type;
    @NonNull protected long ts;
    protected UUID ruleId = null;
    protected T rule = null;

    protected DynamicRuleCommand(DynamicRuleCommandBuilder<T, ?, ?> b) {
        this.id = b.id;
        this.type = b.type;
        this.ts = b.ts;
        this.ruleId = b.ruleId;
        this.rule = b.rule;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0: return id;
            case 1: return type;
            case 2: return ts;
            case 3: return ruleId;
            case 4: return rule;
            default: throw new AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0: id = UUID.fromString(value$.toString()); break;
            case 1: type = DynamicRuleCommandType.valueOf(value$.toString()); break;
            case 2: ts = (long) value$; break;
            case 3: ruleId = UUID.fromString(value$.toString()); break;
            case 4: rule = (T)value$; break;

            default: throw new AvroRuntimeException("Bad index");
        }
    }

}
