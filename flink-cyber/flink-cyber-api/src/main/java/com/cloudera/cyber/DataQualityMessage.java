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

package com.cloudera.cyber;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.flink.api.common.typeinfo.TypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.types.Row;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@TypeInfo(DataQualityMessageTypeInfo.class)
public class DataQualityMessage extends SpecificRecordBase implements SpecificRecord {

    public static final Schema SCHEMA$ = SchemaBuilder
          .record(DataQualityMessage.class.getName())
          .namespace(DataQualityMessage.class.getPackage().getName())
          .fields()
          .requiredString("level")
          .requiredString("feature")
          .requiredString("field")
          .requiredString("message")
          .endRecord();
    public static final TypeInformation<Row> FLINK_TYPE_INFO = Types.ROW_NAMED(
          new String[] {"level", "feature", "field", "message"},
          Types.STRING, Types.STRING, Types.STRING, Types.STRING);
    private String level;
    private String feature;
    private String field;
    private String message;

    public Row toRow() {
        return Row.of(level, feature, field, message);
    }

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    public Object get(int field$) {
        switch (field$) {
            case 0:
                return level;
            case 1:
                return feature;
            case 2:
                return field;
            case 3:
                return message;
            default:
                throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value = "unchecked")
    public void put(int field$, Object value$) {
        switch (field$) {
            case 0:
                level = value$.toString();
                break;
            case 1:
                feature = value$.toString();
                break;
            case 2:
                field = value$.toString();
                break;
            case 3:
                message = value$.toString();
                break;
            default:
                throw new AvroRuntimeException("Bad index");
        }
    }
}
