package com.cloudera.cyber.scoring;

import com.cloudera.cyber.IdentifiedMessage;
import com.cloudera.cyber.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.List;

import static com.cloudera.cyber.AvroTypes.toListOf;

@Data
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ScoredMessage extends SpecificRecordBase implements IdentifiedMessage, SpecificRecord {
    private Message message;
    private List<Scores> cyberScoresDetails;
    private Double cyberScore;

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
            .name("cyberScoresDetails").type(Schema.createArray(Scores.SCHEMA$)).noDefault()
            .optionalDouble("cyberScore")
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
                return cyberScoresDetails;
            case 2:
                return cyberScore;
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
                this.cyberScoresDetails = toListOf(Scores.class, value$);
                break;
            case 2:
                this.cyberScore = (Double) value$;
                break;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }
}
