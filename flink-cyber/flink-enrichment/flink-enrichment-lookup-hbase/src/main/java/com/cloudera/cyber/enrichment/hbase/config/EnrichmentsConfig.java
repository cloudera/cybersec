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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Maps enrichment type to information about the key and value fields of the enrichment and
 * the storage format of the enrichment.
 */
@Data
public class EnrichmentsConfig implements Serializable {
    public static final String NO_STORAGE_TYPE_NAME_SPECIFIED_ERROR = "Null or empty string are not valid storageTypeNames";
    public static final String NO_ENRICHMENT_TYPE_NAME_SPECIFIED_ERROR = "Null or empty string are not valid enrichmentTypeNames";
    public static final String ENRICHMENT_CONFIG_FILE_DESERIALIZATION_ERROR = "Could not deserialize enrichments configuration file '%s'";
    public static final String DEFAULT_ENRICHMENT_STORAGE_NAME = "default";
    public static final String MISSING_STORAGE_ERROR = "Enrichment storage does not contain configuration for %s";

    /**
     * maps a storage config name to a storage Config.
     */
    private HashMap<String, EnrichmentStorageConfig> storageConfigs ;

    /**
     * Maps enrichment type to its configuration
     */
    private HashMap<String, EnrichmentConfig> enrichmentConfigs;

    public EnrichmentsConfig(Map<String, EnrichmentStorageConfig> storageConfigs, Map<String, EnrichmentConfig> enrichmentConfigs) {
        if (storageConfigs != null) {
            this.storageConfigs = new HashMap<>(storageConfigs);
        } else {
            this.storageConfigs = new HashMap<>();
        }

        if (enrichmentConfigs != null) {
            this.enrichmentConfigs = new HashMap<>(enrichmentConfigs);
        } else {
            this.enrichmentConfigs = new HashMap<>();
        }
    }

    public EnrichmentsConfig() {
        this.storageConfigs = new HashMap<>();
        this.enrichmentConfigs = new HashMap<>();
    }

    public static EnrichmentsConfig load(String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            EnrichmentsConfig config = mapper.readValue(new File(filePath), EnrichmentsConfig.class);
            config.validate();
            return config;
        } catch (Exception e) {
            throw new RuntimeException(String.format(ENRICHMENT_CONFIG_FILE_DESERIALIZATION_ERROR, filePath), e);
        }
    }


    public void validate() {
        Preconditions.checkState(storageConfigs.containsKey(DEFAULT_ENRICHMENT_STORAGE_NAME), String.format(MISSING_STORAGE_ERROR, DEFAULT_ENRICHMENT_STORAGE_NAME));
        storageConfigs.forEach((storageType, storageConfig) -> {
            Preconditions.checkState(StringUtils.isNotBlank(storageType), NO_STORAGE_TYPE_NAME_SPECIFIED_ERROR);
            storageConfig.validate(storageType);
        });
        enrichmentConfigs.forEach((enrichmentType, value) -> {
            Preconditions.checkState(StringUtils.isNotBlank(enrichmentType), NO_ENRICHMENT_TYPE_NAME_SPECIFIED_ERROR);
            value.validate(enrichmentType, storageConfigs);
        });
    }

    /**
     * Return a distinct list of tables specified in the storage configs that are referenced by enrichment types.
     * @return list of tables used by enrichment types
     */
    public List<String> getReferencedTables() {
        List<String> referencedStorage = enrichmentConfigs.values().stream().map(EnrichmentConfig::getStorage).distinct().collect(Collectors.toList());
        return getTablesForStorage(referencedStorage);
    }

    public List<String> getReferencedTablesForSource(String source) {
        List<String> storageForSources = enrichmentConfigs.values().stream().filter(c -> c.getFieldMapping().getStreamingSources().contains(source)).map(EnrichmentConfig::getStorage).
                distinct().collect(Collectors.toList());
        return getTablesForStorage(storageForSources);
    }

    private List<String> getTablesForStorage(List<String> storageNames) {
        return storageNames.stream().map(storageConfigs::get).map(EnrichmentStorageConfig::getHbaseTableName).distinct().collect(Collectors.toList());
    }

    public EnrichmentStorageConfig getStorageForEnrichmentType(String enrichmentType) {
        String storageName = DEFAULT_ENRICHMENT_STORAGE_NAME;

        EnrichmentConfig fieldsConfig = enrichmentConfigs.get(enrichmentType);
        if (fieldsConfig != null) {
            storageName = fieldsConfig.getStorage();
        }
        return storageConfigs.get(storageName);
    }

    public List<String> getStreamingEnrichmentSources() {
        return enrichmentConfigs.values().stream().
                flatMap(c -> c.getFieldMapping().getStreamingSources().stream()).distinct().collect(Collectors.toList());
    }

}
