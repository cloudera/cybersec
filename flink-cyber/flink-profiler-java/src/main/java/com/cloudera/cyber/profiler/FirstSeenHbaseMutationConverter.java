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

import com.cloudera.cyber.hbase.LookupKey;
import org.apache.flink.connector.hbase.sink.HBaseMutationConverter;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import static com.cloudera.cyber.enrichment.EnrichmentUtils.CF_ID;
import static com.cloudera.cyber.enrichment.EnrichmentUtils.Q_KEY;

public class FirstSeenHbaseMutationConverter implements HBaseMutationConverter<ProfileMessage> {
    private final FirstSeenHBase firstSeenHBase;

    public FirstSeenHbaseMutationConverter(String hTableName, String columnFamily, ProfileGroupConfig profileGroupConfig) {
        firstSeenHBase = new FirstSeenHBase(hTableName, columnFamily, profileGroupConfig);
    }

    @Override
    public void open() {

    }

    @Override
    public Mutation convertToMutation(ProfileMessage message) {
        String firstSeen = firstSeenHBase.getFirstSeen(message);
        String lastSeen = firstSeenHBase.getLastSeen(message);

        LookupKey key = firstSeenHBase.getKey(message);
        Put put = new Put(key.getKey());
        put.addColumn(CF_ID, Q_KEY, key.getKey());
        put.addColumn(key.getCf(), Bytes.toBytes(FirstSeenHbaseLookup.FIRST_SEEN_PROPERTY_NAME), Bytes.toBytes(firstSeen));
        put.addColumn(key.getCf(), Bytes.toBytes(FirstSeenHbaseLookup.LAST_SEEN_PROPERTY_NAME), Bytes.toBytes(lastSeen));
        return put;
    }
}
