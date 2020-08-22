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

public class EnrichmentEntryTypeFactory extends TypeInfoFactory<EnrichmentEntry> {
    @Override
    public TypeInformation<EnrichmentEntry> createTypeInfo(Type type, Map<String, TypeInformation<?>> map) {
        AvroTypeInfo<EnrichmentEntry> avroOut = new AvroTypeInfo<>(EnrichmentEntry.class);

        List<PojoField> fields = Arrays.stream(avroOut.getFieldNames())
                .map(e ->
                        new PojoField(avroOut.getPojoFieldAt(avroOut.getFieldIndex(e)).getField(),
                                e == "entries" ?
                                        Types.MAP(Types.STRING, Types.STRING) :
                                        avroOut.getPojoFieldAt(avroOut.getFieldIndex(e)).getTypeInformation())
                ).collect(Collectors.toList());

        return new PojoTypeInfo<EnrichmentEntry>(EnrichmentEntry.class, fields);
    }
}