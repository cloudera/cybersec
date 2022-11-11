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

package com.cloudera.cyber.profiler;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.hbase.LookupKey;
import org.apache.flink.connector.hbase.sink.HBaseMutationConverter;
import org.apache.hadoop.hbase.client.Mutation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class FirstSeenHbaseMutationConverter implements HBaseMutationConverter<ProfileMessage> {
    public static final String FIRST_SEEN_ENRICHMENT_TYPE = "first_seen";

    private final FirstSeenHBase firstSeenHBase;
    private final EnrichmentStorageConfig enrichmentStorageConfig;

    public FirstSeenHbaseMutationConverter(EnrichmentStorageConfig enrichmentsStorageConfig, ProfileGroupConfig profileGroupConfig) {
        this.enrichmentStorageConfig = enrichmentsStorageConfig;
        firstSeenHBase = new FirstSeenHBase(enrichmentsStorageConfig, profileGroupConfig);
    }

    @Override
    public void open() {

    }

    @Override
    public Mutation convertToMutation(ProfileMessage message) {
        String firstSeen = firstSeenHBase.getFirstSeen(message);
        String lastSeen = firstSeenHBase.getLastSeen(message);

        LookupKey key = firstSeenHBase.getKey(message);
        Map<String, String> firstSeenMap = new HashMap<>();
        firstSeenMap.put(FirstSeenHbaseLookup.FIRST_SEEN_PROPERTY_NAME, firstSeen);
        firstSeenMap.put(FirstSeenHbaseLookup.LAST_SEEN_PROPERTY_NAME, lastSeen);

        EnrichmentEntry enrichmentEntry = EnrichmentEntry.builder().ts(message.getTs()).
                type(FIRST_SEEN_ENRICHMENT_TYPE).
                key(key.getKey()).entries(firstSeenMap).build();

        EnrichmentCommand enrichmentCommand = EnrichmentCommand.builder().type(CommandType.ADD).
                headers(Collections.emptyMap()).
                payload(enrichmentEntry).build();
        return enrichmentStorageConfig.getFormat().getMutationConverter().convertToMutation(enrichmentStorageConfig, enrichmentCommand);
    }
}
