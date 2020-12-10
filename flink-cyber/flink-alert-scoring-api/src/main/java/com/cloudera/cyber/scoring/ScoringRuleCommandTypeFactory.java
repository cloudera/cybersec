package com.cloudera.cyber.scoring;

import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.avro.typeutils.AvroSchemaConverter;

import java.lang.reflect.Type;
import java.util.Map;

public class ScoringRuleCommandTypeFactory extends TypeInfoFactory<ScoringRuleCommand> {

    @Override
    public TypeInformation<ScoringRuleCommand> createTypeInfo(Type type, Map<String, TypeInformation<?>> map) {
        return AvroSchemaConverter.convertToTypeInfo(ScoringRuleCommand.SCHEMA$.toString());
    }
}
