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

import com.cloudera.cyber.avro.AvroSchemas;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.avro.util.Utf8;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.types.Row;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cloudera.cyber.AvroTypes.toListOf;
import static com.cloudera.cyber.AvroTypes.utf8toStringMap;
import static java.util.stream.Collectors.toMap;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Message extends SpecificRecordBase implements SpecificRecord, IdentifiedMessage, Timestamped {
    @Builder.Default
    @NonNull private String id = UUID.randomUUID().toString();
    private long ts;
    @NonNull private SignedSourceKey originalSource;
    @Builder.Default
    @NonNull private String message = "";
    private Map<String, List<ThreatIntelligence>> threats;
    private Map<String, String> extensions;
    @NonNull private String source;
    private List<DataQualityMessage> dataQualityMessages;

    public static final Schema SCHEMA$ = AvroSchemas.createRecordBuilder(Message.class.getPackage().getName(), Message.class.getName())
            .fields()
            .requiredString("id")
            .name("ts").type(LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG))).noDefault()
            .name("originalSource").type(SignedSourceKey.SCHEMA$).noDefault()
            .requiredString("message")
            .name("threats").type().optional().type(SchemaBuilder.map().values(SchemaBuilder.array().items(ThreatIntelligence.SCHEMA$)))
            .name("extensions").type(Schema.createMap(Schema.create(Schema.Type.STRING))).noDefault()
            .requiredString("source")
            .name("dataQualityMessages").type().optional().type(Schema.createArray(DataQualityMessage.SCHEMA$))
            .endRecord();

    public static final TypeInformation<Row> FLINK_TYPE_INFO = Types.ROW_NAMED(
            new String[]{"id", "ts", "originalSource", "message", "threats", "extensions", "source", "dataQualityMessages"},
            Types.STRING, Types.LONG, SignedSourceKey.FLINK_TYPE_INFO,Types.STRING,
            Types.MAP(Types.STRING, Types.OBJECT_ARRAY(ThreatIntelligence.FLINK_TYPE_INFO)),
            Types.MAP(Types.STRING, Types.STRING), Types.STRING, Types.OBJECT_ARRAY(DataQualityMessage.FLINK_TYPE_INFO));

    public Row toRow() {
        return Row.of(id,
                ts,
                originalSource.toRow(),
                message,
                threats == null ? null : threats.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().stream()
                                        .map(ThreatIntelligence::toRow)
                                        .toArray(Row[]::new))),
                extensions,
                source,
                dataQualityMessages == null ? null : dataQualityMessages.stream()
                        .map(DataQualityMessage::toRow)
                        .toArray(Row[]::new)
        );
    }

    public static Schema getClassSchema() {
        return SCHEMA$;
    }

    @Override
    public Schema getSchema() {
        return SCHEMA$;
    }

    public java.lang.Object get(int field$) {
        switch (field$) {
            case 0: return id;
            case 1: return ts;
            case 2: return originalSource;
            case 3: return message;
            case 4: return threats;
            case 5: return extensions;
            case 6: return source;
            case 7: return dataQualityMessages == null ? null : dataQualityMessages instanceof List ? dataQualityMessages : Arrays.asList(dataQualityMessages);
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    // Used by DatumReader.  Applications should not call.
    @SuppressWarnings(value="unchecked")
    public void put(int field$, java.lang.Object value$) {
        switch (field$) {
            case 0: id = value$.toString(); break;
            case 1: ts = (java.lang.Long)value$; break;
            case 2: originalSource = (com.cloudera.cyber.SignedSourceKey)value$; break;
            case 3:
                message = value$.toString();
                break;
            case 4: threats = toTiMap(value$); break;
            case 5: extensions = utf8toStringMap(value$); break;
            case 6: source = value$.toString(); break;
            case 7: dataQualityMessages = toListOf(DataQualityMessage.class, value$); break;
            default: throw new org.apache.avro.AvroRuntimeException("Bad index");
        }
    }

    private Map<String, List<ThreatIntelligence>> toTiMap(Object value$) {
        if(value$ == null) return null;
        Map<Utf8, List<Object>> o = (Map<Utf8, List<Object>>) value$;
        return o.entrySet().stream().collect(toMap(
                k -> k.getKey().toString(),
                v -> toListOf(ThreatIntelligence.class, v.getValue())
        ));
    }
}
