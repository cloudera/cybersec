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

package com.cloudera.cyber.test.generator;

import java.nio.charset.StandardCharsets;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;

public class AvroMapFunction extends RichMapFunction<Tuple2<String, String>, Tuple2<String, byte[]>> {

    private final String schemaString;
    private transient Schema schema;

    AvroMapFunction(String schemaString) {
        this.schemaString = schemaString;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.schema =  new Parser().parse(schemaString);

    }

    @Override
    public Tuple2<String, byte[]> map(Tuple2<String, String> tuple2) {
        return new Tuple2<>(tuple2.f0, Utils.jsonDecodeToAvroByteArray(tuple2.f1, schema));
    }
}
