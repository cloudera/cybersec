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

import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

import java.io.ByteArrayInputStream;
import java.util.Collections;

public class ThreatQParserFlatMap implements FlatMapFunction<String, EnrichmentCommand> {

    @Override
    public void flatMap(String s, Collector<EnrichmentCommand> collector) throws Exception {
        ThreatQParser.parse(new ByteArrayInputStream(s.getBytes())).forEach(out ->
                collector.collect(EnrichmentCommand.builder().headers(Collections.emptyMap()).
                        type(CommandType.ADD).
                        payload(ThreatQEntry.toEnrichmentEntry(out)).build()));
    }
}
