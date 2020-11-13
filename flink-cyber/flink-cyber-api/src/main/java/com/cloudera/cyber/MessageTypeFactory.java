package com.cloudera.cyber;

import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.typeutils.PojoField;
import org.apache.flink.api.java.typeutils.PojoTypeInfo;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MessageTypeFactory extends TypeInfoFactory<Message> {
    @Override
    public TypeInformation<Message> createTypeInfo(Type type, Map<String, TypeInformation<?>> map) {
        AvroTypeInfo<Message> avroOut = new AvroTypeInfo<>(Message.class);

        List<PojoField> fields = Arrays.stream(avroOut.getFieldNames())
                .map(e -> new PojoField(avroOut.getPojoFieldAt(avroOut.getFieldIndex(e)).getField(),
                                e == "entries" ?
                                        Types.MAP(Types.STRING, Types.GENERIC(Object.class)) :
                                        avroOut.getTypeAt(e))
                ).collect(Collectors.toList());

        return new PojoTypeInfo<>(Message.class, fields);
    }
}
