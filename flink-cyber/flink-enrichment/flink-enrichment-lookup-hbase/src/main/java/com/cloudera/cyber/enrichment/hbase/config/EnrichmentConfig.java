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

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;

import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig.DEFAULT_ENRICHMENT_STORAGE_NAME;

@Data
@Slf4j
@AllArgsConstructor
@NoArgsConstructor
public class EnrichmentConfig implements Serializable {
    public static final String ENRICHMENT_CONFIG_MISSING_STORAGE_ERROR = "EnrichmentConfig %s: references undefined storage %s.";
    public static final String ENRICHMENT_CONFIG_MISSING_FIELD = "EnrichmentConfig %s, field mapping not specified";
    private String storage;
    private EnrichmentFieldsConfig fieldMapping;


    public void validate(String enrichmentType, Map<String, EnrichmentStorageConfig> storageConfigs) {
        Preconditions.checkState(storage == null || storageConfigs.containsKey(storage), String.format(ENRICHMENT_CONFIG_MISSING_STORAGE_ERROR, enrichmentType, storage));
        Preconditions.checkNotNull(fieldMapping, String.format(ENRICHMENT_CONFIG_MISSING_FIELD, enrichmentType));
        fieldMapping.validate(enrichmentType);
    }

    public String getStorage() {
        if (storage == null) {
            return DEFAULT_ENRICHMENT_STORAGE_NAME;
        } else {
            return storage;
        }
    }
}
