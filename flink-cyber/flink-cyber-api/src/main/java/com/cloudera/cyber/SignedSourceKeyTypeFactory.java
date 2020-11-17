package com.cloudera.cyber;

import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SignedSourceKeyTypeFactory extends TypeInfoFactory<SignedSourceKey> {
    @Override
    public TypeInformation<SignedSourceKey> createTypeInfo(Type type, Map<String, TypeInformation<?>> map) {
        return Types.POJO(SignedSourceKey.class, new HashMap<String, TypeInformation<?>>() {{
            put("topic", Types.STRING);
            put("partition", Types.INT);
            put("offset", Types.LONG);
            put("signature", Types.PRIMITIVE_ARRAY(Types.BYTE));
        }});
    }
}
