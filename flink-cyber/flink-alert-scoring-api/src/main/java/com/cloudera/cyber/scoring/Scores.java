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

import com.cloudera.cyber.AvroTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.types.Row;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Scores extends SpecificRecordBase implements SpecificRecord {
    private String ruleId;
    private Double score;
    private String reason;

    public static final Schema SCHEMA$ = SchemaBuilder.record(Scores.class.getName())
            .namespace(Scores.class.getPackage().getName())
            .fields()
            .name("ruleId").type(AvroTypes.uuidType).noDefault()
            .requiredDouble("score")
            .requiredString("reason")
            .endRecord();

    public static final TypeInformation<Row> FLINK_TYPE_INFO = Types.ROW_NAMED(
            new String[]{"ruleId", "score", "reason"},
            Types.STRING, Types.DOUBLE, Types.STRING);

    public Row toRow() {
        return Row.of(ruleId, score, reason);
    }

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    @Override
    public Object get(int field$) {
        switch (field$) {
            case 0:
                return ruleId;
            case 1:
                return score;
            case 2:
                return reason;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }

    @Override
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0:
                ruleId = value$.toString();
                break;
            case 1:
                score = (Double) value$;
                break;
            case 2:
                reason = value$.toString();
                break;
            default:
                throw new AvroRuntimeException("Bad Index");
        }
    }
}
