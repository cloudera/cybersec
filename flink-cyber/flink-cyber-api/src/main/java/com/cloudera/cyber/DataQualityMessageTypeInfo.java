package com.cloudera.cyber;

import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;

import java.lang.reflect.Type;
import java.util.Map;

public class DataQualityMessageTypeInfo extends TypeInfoFactory<DataQualityMessage> {
    @Override
    public TypeInformation<DataQualityMessage> createTypeInfo(Type type, Map<String, TypeInformation<?>> map) {
        return new AvroTypeInfo<>(DataQualityMessage.class);
    }
}
