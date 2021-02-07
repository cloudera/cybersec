package com.cloudera.cyber.scoring;

import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;

import java.lang.reflect.Type;
import java.util.Map;

public class ScoringRuleTypeFactory extends TypeInfoFactory<ScoringRule> {

    @Override
    public TypeInformation<ScoringRule> createTypeInfo(Type type, Map<String, TypeInformation<?>> map) {
        return new AvroTypeInfo<>(ScoringRule.class);
    }
}
