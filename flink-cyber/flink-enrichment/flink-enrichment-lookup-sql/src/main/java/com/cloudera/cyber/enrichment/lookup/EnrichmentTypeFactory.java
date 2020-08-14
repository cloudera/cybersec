package com.cloudera.cyber.enrichment.lookup;

import com.google.common.collect.ImmutableMap;
import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;

import java.lang.reflect.Type;
import java.util.Map;

public class EnrichmentTypeFactory extends TypeInfoFactory<Enrichment> {
    @Override
    public TypeInformation<Enrichment> createTypeInfo(Type type, Map<String, TypeInformation<?>> map) {
        return Types.POJO(Enrichment.class,
                ImmutableMap.of("ts", Types.LONG,
                        "pri", Types.STRING,
                        "entries", Types.MAP(Types.STRING, Types.STRING)
                ));
    }
}
