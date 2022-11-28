/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

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
