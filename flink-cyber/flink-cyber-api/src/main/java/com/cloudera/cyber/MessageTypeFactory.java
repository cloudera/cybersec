package com.cloudera.cyber;

import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.cloudera.cyber.AvroTypes.overrideTypes;

public class MessageTypeFactory extends TypeInfoFactory<Message> {
    @Override
    public TypeInformation<Message> createTypeInfo(Type type, Map<String, TypeInformation<?>> map) {
        return overrideTypes(new AvroTypeInfo<>(Message.class),
                Message.class,
                new HashMap<String, TypeInformation<?>>() {
                    {
                        put("dataQualityMessages", Types.OBJECT_ARRAY(TypeInformation.of(DataQualityMessage.class)));
                        put("extensions", Types.MAP(Types.STRING, Types.STRING));
                        put("originalSource", overrideTypes(new AvroTypeInfo<>(SignedSourceKey.class),
                                SignedSourceKey.class,
                                Collections.singletonMap("signature", Types.PRIMITIVE_ARRAY(Types.BYTE))));
                        put("threats", Types.OBJECT_ARRAY(overrideTypes(new AvroTypeInfo<>(ThreatIntelligence.class),
                                ThreatIntelligence.class,
                                Collections.singletonMap("fields", Types.MAP(Types.STRING, Types.STRING)))));
                    }
                });
    }
}
