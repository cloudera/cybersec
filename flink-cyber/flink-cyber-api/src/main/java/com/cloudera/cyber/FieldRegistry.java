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

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;

import java.util.HashMap;
import java.util.Map;

public class FieldRegistry implements IFieldRegistry {

    private Map<String, Field> fields = new HashMap<>();

    private static FieldRegistry instance;

    public static FieldRegistry instance() {
        if(instance == null) {
            instance = new FieldRegistry();
        }
        return instance;
    }


    @Override
    public void addSchema(SchemaBuilder.FieldAssembler<Schema> builder) {

    }

    @Override
    public boolean contains(String key) {
        return fields.containsKey(key);
    }
}
