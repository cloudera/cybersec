package com.cloudera.cyber;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.avro.util.Utf8;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

public class AvroTypes {
    public static final Schema timestampMilliType = LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG));
    public static final Schema uuidType = LogicalTypes.uuid().addToSchema(Schema.create(Schema.Type.STRING));

    public static <T extends SpecificRecordBase> TypeInformation<T> overrideTypes(AvroTypeInfo<T> avro, Class<T> clazz, Map<String, TypeInformation<?>> fields) {
        return Types.POJO(clazz, Arrays.stream(avro.getFieldNames())
                .collect(toMap(
                        k -> k,
                        v -> fields.containsKey(v) ? fields.get(v) :
                                avro.getTypeAt(v)
                )));
    }

    public static Map<String, String> utf8toStringMap(Object value$) {
        if (value$ == null) return null;
        try {
            return ((Map<Utf8, Utf8>) value$).entrySet().stream().collect(toMap(
                    k -> k.getKey().toString(),
                    k -> k.getValue().toString()
            ));
        } catch (ClassCastException e) {
            return (Map<String, String>) value$;
        }
    }

    public static <T> List<T> toListOf(Class<T> cls, Object value$) {
        // TODO - ensure the serialization of the contained object is correct
        if (value$ == null) return null;
        return ((List<Object>) value$).stream()
                .map(o -> (T) o)
                .collect(toList());
    }
}
