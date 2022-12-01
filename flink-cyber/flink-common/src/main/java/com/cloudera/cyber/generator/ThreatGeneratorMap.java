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

package com.cloudera.cyber.generator;

import com.cloudera.cyber.generator.ThreatGenerator;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;

public class ThreatGeneratorMap extends RichMapFunction<String, Tuple2<String, String>> {
    private transient ThreatGenerator threatGenerator;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        threatGenerator = new ThreatGenerator();
    }

    @Override
    public Tuple2<String, String> map(String ip) throws Exception {
        return Tuple2.of("threats", threatGenerator.generateThreat(ip));
    }
}
