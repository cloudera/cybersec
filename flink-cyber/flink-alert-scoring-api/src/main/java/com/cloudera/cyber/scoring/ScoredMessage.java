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

import com.cloudera.cyber.IdentifiedMessage;
import com.cloudera.cyber.Message;
import lombok.*;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;

import java.util.DoubleSummaryStatistics;
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

    public DoubleSummaryStatistics getSummaryPositive() {
        return cyberScoresDetails.stream().mapToDouble(s -> s.getScore()).filter(d -> d > 0).summaryStatistics();
    }

    public DoubleSummaryStatistics getSummaryNegative() {
        return cyberScoresDetails.stream().mapToDouble(s -> s.getScore()).filter(d -> d > 0).summaryStatistics();
    }
    public boolean isCyberAlert() {
        return (getSummaryPositive().getAverage() > -getSummaryNegative().getAverage());
    }

    public Double getCyberScore() {
        return isCyberAlert() ? getSummaryPositive().getAverage(): 0.0;
    }

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
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }
}
