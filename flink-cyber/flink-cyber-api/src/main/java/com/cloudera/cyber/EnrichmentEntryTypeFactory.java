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