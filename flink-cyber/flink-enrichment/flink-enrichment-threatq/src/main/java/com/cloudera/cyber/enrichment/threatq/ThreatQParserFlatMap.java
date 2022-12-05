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

package com.cloudera.cyber.enrichment.threatq;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

import java.io.ByteArrayInputStream;

public class ThreatQParserFlatMap implements FlatMapFunction<String, ThreatQEntry> {
    @Override
    public void flatMap(String s, Collector<ThreatQEntry> collector) throws Exception {
        ThreatQParser.parse(new ByteArrayInputStream(s.getBytes())).forEach(out -> collector.collect(out));
    }
}
