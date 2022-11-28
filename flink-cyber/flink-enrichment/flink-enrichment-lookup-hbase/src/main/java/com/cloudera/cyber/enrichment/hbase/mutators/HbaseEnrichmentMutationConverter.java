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
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import org.apache.flink.connector.hbase.sink.HBaseMutationConverter;
import org.apache.hadoop.hbase.client.Mutation;

public class HbaseEnrichmentMutationConverter implements HBaseMutationConverter<EnrichmentCommand> {
    private final EnrichmentsConfig enrichmentsConfig;

    public HbaseEnrichmentMutationConverter(EnrichmentsConfig enrichmentsConfig) {
        this.enrichmentsConfig = enrichmentsConfig;
    }

    @Override
    public void open() {
    }

    @Override
    public Mutation convertToMutation(EnrichmentCommand enrichmentCommand) {
        EnrichmentStorageConfig storageConfig = enrichmentsConfig.getStorageForEnrichmentType(enrichmentCommand.getPayload().getType());
        EnrichmentCommandMutationConverter mutationConverter = storageConfig.getFormat().getMutationConverter();
        return mutationConverter.convertToMutation(storageConfig, enrichmentCommand);
    }
}
