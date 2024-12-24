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

package com.cloudera.cyber.enrichment.hbase.mutators;

import static com.cloudera.cyber.enrichment.EnrichmentUtils.CF_ID;
import static com.cloudera.cyber.enrichment.EnrichmentUtils.Q_KEY;
import static com.cloudera.cyber.enrichment.EnrichmentUtils.enrichmentKey;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

public class SimpleHbaseEnrichmentMutationConverter implements EnrichmentCommandMutationConverter {
    @Override
    public Mutation convertToMutation(EnrichmentStorageConfig storageConfig, EnrichmentCommand enrichmentCommand) {
        // For each incoming query result we create a Put operation
        EnrichmentEntry enrichmentEntry = enrichmentCommand.getPayload();
        switch (enrichmentCommand.getType()) {
            case ADD:
                Put put = new Put(enrichmentKey(enrichmentEntry.getKey()));
                put.addColumn(CF_ID, Q_KEY, Bytes.toBytes(enrichmentEntry.getKey()));
                // add the map for the entries
                enrichmentEntry.getEntries().forEach(
                      (k, v) -> put.addColumn(Bytes.toBytes(enrichmentEntry.getType()), Bytes.toBytes(k),
                            Bytes.toBytes(v)));
                return put;
            case DELETE:
                return new Delete(Bytes.toBytes(enrichmentEntry.getKey()));
            default:
                break;
        }

        // this should not happen - Enrichment commands filtered into ADD and DELETE before sink
        return null;
    }
}
