package com.cloudera.cyber;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;

import java.util.Arrays;
import java.util.Map;

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
}
