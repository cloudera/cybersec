package com.cloudera.cyber;

import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static com.cloudera.cyber.AvroTypes.overrideTypes;

public class ThreatIntelligenceTypeFactory extends TypeInfoFactory<ThreatIntelligence> {
    @Override
    public TypeInformation<ThreatIntelligence> createTypeInfo(Type type, Map<String, TypeInformation<?>> map) {
        return overrideTypes(new AvroTypeInfo<>(ThreatIntelligence.class),
                ThreatIntelligence.class,
                new HashMap<String, TypeInformation<?>>() {
                    {
                        put("fields", Types.MAP(Types.STRING, Types.STRING));
                    }
                });
    }
}
