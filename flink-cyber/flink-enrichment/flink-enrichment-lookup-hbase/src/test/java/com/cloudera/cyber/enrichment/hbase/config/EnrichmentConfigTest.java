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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig.DEFAULT_ENRICHMENT_STORAGE_NAME;
import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfigTest.DEFAULT_STORAGE_CONFIGS;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnrichmentConfigTest {

    private static final ArrayList<String> KEY_FIELDS = new ArrayList<>(Collections.singletonList("key_1"));
    private static final ArrayList<String> VALUE_FIELDS = new ArrayList<>(Collections.singletonList("value_1"));
    private static final String TEST_ENRICHMENT_TYPE = "test_enrichment";

    @Test
    public void testValidConfigs() {
        EnrichmentFieldsConfig fieldsConfig = new EnrichmentFieldsConfig(KEY_FIELDS, ":", VALUE_FIELDS, null);
        testEnrichmentConfig(null, DEFAULT_STORAGE_CONFIGS, fieldsConfig);
        testEnrichmentConfig(DEFAULT_ENRICHMENT_STORAGE_NAME, DEFAULT_STORAGE_CONFIGS, fieldsConfig);
    }

    @Test
    public void testUndefinedStorageConfig() {
       EnrichmentFieldsConfig fieldsConfig = new EnrichmentFieldsConfig(KEY_FIELDS, ":", VALUE_FIELDS, null);
       testInvalidConfig(IllegalStateException.class, EnrichmentConfig.ENRICHMENT_CONFIG_MISSING_STORAGE_ERROR, "undefined", fieldsConfig);
    }

    @Test
    public void testNullFieldsConfig() {
        testInvalidConfig(NullPointerException.class, EnrichmentConfig.ENRICHMENT_CONFIG_MISSING_FIELD, null, null);
    }

    @Test
    public void testInvalidFieldsConfig() {
        EnrichmentFieldsConfig invalidFieldsConfig = new EnrichmentFieldsConfig(null, ":", VALUE_FIELDS, null);
        testFieldsConfig(invalidFieldsConfig);
    }

    private void testEnrichmentConfig(String storage, Map<String, EnrichmentStorageConfig> storageConfigs, EnrichmentFieldsConfig fieldsConfig) {
        EnrichmentConfig enrichmentConfig = new EnrichmentConfig(storage, fieldsConfig);
        enrichmentConfig.validate(TEST_ENRICHMENT_TYPE, storageConfigs);
    }

    private void testFieldsConfig(EnrichmentFieldsConfig fieldsConfig) {
        testInvalidConfig(IllegalStateException.class, EnrichmentFieldsConfig.FIELD_CONFIG_INVALID_KEY_FIELD, null, fieldsConfig);
    }

    private void testInvalidConfig(Class<? extends Exception> exceptionClass, String expectedMessage, String storage, EnrichmentFieldsConfig fieldsConfig) {
        assertThatThrownBy(() -> testEnrichmentConfig(storage, EnrichmentsConfigTest.DEFAULT_STORAGE_CONFIGS, fieldsConfig)).isInstanceOf(exceptionClass)
                .hasMessage(expectedMessage, TEST_ENRICHMENT_TYPE, storage);
    }
}
