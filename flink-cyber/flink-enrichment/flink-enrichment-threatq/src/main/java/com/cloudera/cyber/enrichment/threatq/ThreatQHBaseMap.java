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

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentFieldsConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.hbase.AbstractHbaseMapFunction;
import com.cloudera.cyber.hbase.LookupKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.util.Collector;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Slf4j
public class ThreatQHBaseMap extends AbstractHbaseMapFunction<Message, Message> {

    private final List<ThreatQConfig> configs;
    private final EnrichmentStorageConfig threatqStorage;

    public ThreatQHBaseMap(List<ThreatQConfig> configs, EnrichmentsConfig enrichmentStorageConfig) {
        super();
        this.configs = configs;
        log.info("ThreatQ Configuration: {}", configs);
        this.threatqStorage = enrichmentStorageConfig.getStorageForEnrichmentType(EnrichmentFieldsConfig.THREATQ_ENRICHMENT_NAME);
    }

    @Override
    public void processElement(Message message, Context context, Collector<Message> collector) {
        if (this.configs == null) {
            collector.collect(message);
        } else {
            Map<String, String> results = configs.stream()
                    .map(config -> {
                        String f = config.getField();
                        if (!message.getExtensions().containsKey(f)) {
                            return Collections.<String, String>emptyMap();
                        }
                        String k = ThreatQEntry.createKey(config.getIndicatorType(), message.getExtensions().get(f));
                        LookupKey lookup = threatqStorage.getFormat().getLookupBuilder().build(threatqStorage, EnrichmentFieldsConfig.THREATQ_ENRICHMENT_NAME, k);
                        return hbaseLookup(message.getTs(), lookup, f + ".threatq");
                    })
                    .flatMap(m -> m.entrySet().stream())
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

            collector.collect(MessageUtils.addFields(message, results));
        }
    }

}
