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

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentField;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentKind;
import com.cloudera.cyber.hbase.LookupKey;

import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import com.cloudera.cyber.hbase.AbstractHbaseMapFunction;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Getter
public class HbaseEnrichmentMapFunction extends AbstractHbaseMapFunction<Message, Message> {
    private final Map<String, List<EnrichmentField>> fieldToLookup;
    private final Set<String> sources;
    private final String tableName;

    public HbaseEnrichmentMapFunction(List<EnrichmentConfig> configs, String tableName) {
        super();

        sources = configs.stream()
                .filter(c -> c.getKind().equals(EnrichmentKind.HBASE))
                .map(EnrichmentConfig::getSource)
                .collect(Collectors.toSet());

        fieldToLookup = configs.stream()
                .filter(c -> c.getKind().equals(EnrichmentKind.HBASE))
                .collect(toMap(
                        EnrichmentConfig::getSource, EnrichmentConfig::getFields)
                );

        log.info("Applying HBase enrichments to the following sources: {}", sources);

        this.tableName = tableName;
    }

    @Override
    public Message map(Message message) {
        if (sources.stream().noneMatch(s -> message.getSource().equals(s))) return message;

        messageCounter.inc(1);

        return MessageUtils.addFields(message, fieldToLookup.get(message.getSource()).stream().map(
                field -> {
                    byte[] cf = Bytes.toBytes(field.getEnrichmentType());
                    String key = message.getExtensions().get(field.getName());
                    return ((key == null) ? notFound() : hbaseLookup(message.getTs(),
                            LookupKey.builder()
                                    .key(Bytes.toBytes(key))
                                    .cf(cf)
                                    .build(),
                            Joiner.on(".").join(field.getName(), field.getEnrichmentType())
                    )).entrySet().stream();
                }).flatMap(l -> l)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b)));
    }

}

