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
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.hbase.AbstractHbaseMapFunction;
import com.cloudera.cyber.hbase.LookupKey;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Slf4j
public class ThreatQHBaseMap extends AbstractHbaseMapFunction<Message, Message> {
    private static final String THREAT_Q_ENRICHMENT_NAME = "threatq";
    private static final String DEFAULT_THREATQ_TABLE = "threatq";
    private static final String DEFAULT_THREATQ_COLUMN_FAMILY = "t";

    private final List<ThreatQConfig> configs;
    private final EnrichmentStorageConfig threatqStorage;

    public ThreatQHBaseMap(List<ThreatQConfig> configs, EnrichmentsConfig enrichmentStorageConfig) {
        super();
        this.configs = configs;
        log.info("ThreatQ Configuration: {}", configs);
        this.threatqStorage = enrichmentStorageConfig.getStorageConfigs().
                getOrDefault(THREAT_Q_ENRICHMENT_NAME,
                        new EnrichmentStorageConfig(EnrichmentStorageFormat.HBASE_SIMPLE, DEFAULT_THREATQ_TABLE, DEFAULT_THREATQ_COLUMN_FAMILY));
    }

    @Override
    public Message map(Message message) {
        if (this.configs == null) return message;

        Map<String, String> results = configs.stream()
                .map(config -> {
                    String f = config.getField();
                    if (!message.getExtensions().containsKey(f)) {
                        return Collections.<String,String>emptyMap();
                    }
                    String k = config.getIndicatorType() + ":" + message.getExtensions().get(f);
                    LookupKey lookup = threatqStorage.getFormat().getLookupBuilder().build(threatqStorage, THREAT_Q_ENRICHMENT_NAME, k);
                    return hbaseLookup(message.getTs(), lookup, f + ".threatq");
                })
                .flatMap(m -> m.entrySet().stream())
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        return MessageUtils.addFields(message, results);
    }

}
