package com.cloudera.cyber.scoring;

import com.cloudera.cyber.IdentifiedMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.rules.RuleType;
import lombok.*;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@Getter
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ScoredMessage extends SpecificRecordBase implements IdentifiedMessage, SpecificRecord {
    private Message message;
    private List<Scores> scores;
    private List<ScoringRule> rules;

    @Override
    public String getId() {
        return message.getId();
    }

    @Override
    public long getTs() {
        return message.getTs();
    }

    public static final Schema SCHEMA$ = SchemaBuilder.record(ScoredMessage.class.getName())
            .namespace(ScoredMessage.class.getPackage().getName())
            .fields()
            .name("message").type(Message.SCHEMA$).noDefault()
            .name("scores").type(Schema.createArray(Scores.SCHEMA$)).noDefault()
            .name("rules").type(Schema.createArray(ScoringRule.SCHEMA$)).noDefault()
            .endRecord();

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0:
                return message;
            case 1:
                return scores;
            case 2:
                return rules;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0:
                this.message = (Message) value$;
                break;
            case 1:
                this.scores = (List<Scores>) value$;
                break;
            case 2:
                this.rules = (List<ScoringRule>) value$;
                break;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }
}
