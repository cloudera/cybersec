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

package com.cloudera.cyber.enrichment.hbase;

import static java.util.stream.Collectors.toMap;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.cloudera.cyber.hbase.AbstractHbaseMapFunction;
import com.cloudera.cyber.hbase.LookupKey;
import com.google.common.base.Joiner;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.flink.util.Collector;

@Slf4j
@Getter
public class HbaseEnrichmentMapFunction extends AbstractHbaseMapFunction<Message, Message> {
    private final Map<String, List<EnrichmentField>> fieldToLookup;
    private final EnrichmentsConfig enrichmentStorageConfig;

    public HbaseEnrichmentMapFunction(List<EnrichmentConfig> configs,
                                      com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig enrichmentStorageConfig) {
        super();

        fieldToLookup = configs.stream()
                               .filter(c -> c.getKind().equals(EnrichmentKind.HBASE))
                               .collect(toMap(
                                     EnrichmentConfig::getSource, EnrichmentConfig::getFields)
                               );

        log.info("Applying HBase enrichments to the following sources: {}", String.join(" ,", fieldToLookup.keySet()));

        this.enrichmentStorageConfig = enrichmentStorageConfig;
    }

    private LookupKey buildLookup(String enrichmentType, String fieldValue) {
        EnrichmentStorageConfig storageConfig = enrichmentStorageConfig.getStorageForEnrichmentType(enrichmentType);
        return storageConfig.getFormat().getLookupBuilder().build(storageConfig, enrichmentType, fieldValue);
    }

    @Override
    public void processElement(Message message, Context context, Collector<Message> collector) {
        List<EnrichmentField> enrichmentFields = fieldToLookup.get(message.getSource());
        if (CollectionUtils.isNotEmpty(enrichmentFields)) {
            messageCounter.inc(1);
            collector.collect(
                  MessageUtils.addFields(message, enrichmentFields.stream().flatMap(
                                                                        field -> {
                                                                            String enrichmentType = field.getEnrichmentType();
                                                                            String key = message.getExtensions().get(field.getName());

                                                                            if (key == null) {
                                                                                return notFound().entrySet().stream();
                                                                            }
                                                                            return hbaseLookup(message.getTs(),
                                                                                  buildLookup(enrichmentType, key),
                                                                                  Joiner.on(".")
                                                                                        .join(field.getName(), field.getEnrichmentType())
                                                                            ).entrySet().stream();
                                                                        })
                                                                  .collect(toMap(Map.Entry::getKey, Map.Entry::getValue,
                                                                        (a, b) -> b))));
        } else {
            collector.collect(message);
        }
    }
}

