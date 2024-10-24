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

import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import java.util.HashMap;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.metron.enrichment.converter.EnrichmentConverter;
import org.apache.metron.enrichment.converter.EnrichmentKey;
import org.apache.metron.enrichment.converter.EnrichmentValue;

public class MetronHbaseEnrichmentMutationConverter implements EnrichmentCommandMutationConverter {
    private final EnrichmentConverter converter = new EnrichmentConverter();

    @Override
    public Mutation convertToMutation(EnrichmentStorageConfig storageConfig, EnrichmentCommand enrichmentCommand) {

        EnrichmentKey key =
              new EnrichmentKey(enrichmentCommand.getPayload().getType(), enrichmentCommand.getPayload().getKey());
        EnrichmentValue value = new EnrichmentValue(new HashMap<>(enrichmentCommand.getPayload().getEntries()));

        return converter.toPut(storageConfig.getColumnFamily(), key, value);
    }

}
