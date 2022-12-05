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

package com.cloudera.cyber.enrichment.hbase.config;

import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.hbase.mutators.EnrichmentCommandMutationConverter;
import com.cloudera.cyber.enrichment.hbase.mutators.MetronHbaseEnrichmentMutationConverter;
import com.cloudera.cyber.enrichment.hbase.mutators.SimpleHbaseEnrichmentMutationConverter;

public enum EnrichmentStorageFormat {
    HBASE_METRON(new MetronHbaseEnrichmentMutationConverter()),
    HBASE_SIMPLE(new SimpleHbaseEnrichmentMutationConverter());

    private final EnrichmentCommandMutationConverter mutationConverter;

    EnrichmentStorageFormat(EnrichmentCommandMutationConverter mutationConverter) {
        this.mutationConverter = mutationConverter;
    }

    public EnrichmentCommandMutationConverter getMutationConverter() {
        return mutationConverter;
    }
}
