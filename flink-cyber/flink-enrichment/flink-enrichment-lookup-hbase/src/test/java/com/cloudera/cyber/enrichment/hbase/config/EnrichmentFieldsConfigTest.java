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

import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EnrichmentFieldsConfigTest {

    private static final String TEST_ENRICHMENT_TYPE = "test_enrichment";
    public static final ArrayList<String> KEY_FIELDS = new ArrayList<>(Collections.singletonList("key_1"));
    public static final ArrayList<String> DUPLICATE_FIELDS = Lists.newArrayList("same", "same");
    private static final ArrayList<String> VALUE_FIELDS = new ArrayList<>(Collections.singletonList("value_1"));
    private static final ArrayList<String> EMPTY_LIST = new ArrayList<>();

    @Test
    public void testValidConfigs() {
        testValidConfigs(null);
        testValidConfigs("");
    }

    @Test
    public void testInvalidKeyField() {
        testInvalidFieldsConfig(EnrichmentFieldsConfig.FIELD_CONFIG_INVALID_KEY_FIELD, null, null);
        testInvalidFieldsConfig(EnrichmentFieldsConfig.FIELD_CONFIG_INVALID_KEY_FIELD, EMPTY_LIST, null);
        testInvalidFieldsConfig(EnrichmentFieldsConfig.FIELD_CONFIG_INVALID_VALUE_FIELD, KEY_FIELDS, EMPTY_LIST);
        testInvalidFieldsConfig(EnrichmentFieldsConfig.FIELD_CONFIG_DUPLICATE_KEY_FIELD, DUPLICATE_FIELDS, null);
        testInvalidFieldsConfig(EnrichmentFieldsConfig.FIELD_CONFIG_DUPLICATE_VALUE_FIELD, KEY_FIELDS, DUPLICATE_FIELDS);
    }

    @Test
    public void testInvalidValueField() {
        testInvalidFieldsConfig(EnrichmentFieldsConfig.FIELD_CONFIG_INVALID_VALUE_FIELD, KEY_FIELDS, EMPTY_LIST);
        testInvalidFieldsConfig(EnrichmentFieldsConfig.FIELD_CONFIG_DUPLICATE_VALUE_FIELD, KEY_FIELDS, DUPLICATE_FIELDS);
    }

    private void testValidConfigs(String keyDelimiter) {
        testFieldsConfig(KEY_FIELDS, keyDelimiter, null);
        testFieldsConfig(KEY_FIELDS, ":", null);
        testFieldsConfig(KEY_FIELDS, keyDelimiter, VALUE_FIELDS);
        testFieldsConfig(KEY_FIELDS, keyDelimiter, VALUE_FIELDS);
    }

    private void testFieldsConfig(ArrayList<String> keyFields, String keyDelimiter, ArrayList<String> valueFields) {
        EnrichmentFieldsConfig fieldConfig = new EnrichmentFieldsConfig(keyFields, keyDelimiter, valueFields, null);
        fieldConfig.validate(TEST_ENRICHMENT_TYPE);
        if (keyDelimiter == null) {
            Assert.assertEquals(EnrichmentFieldsConfig.DEFAULT_KEY_DELIMITER, fieldConfig.getKeyDelimiter());
        } else {
            Assert.assertEquals(keyDelimiter, fieldConfig.getKeyDelimiter());
        }
    }

    private void testInvalidFieldsConfig(String expectedMessage, ArrayList<String> keyFields, ArrayList<String> valueFields) {
        assertThatThrownBy(() -> testFieldsConfig(keyFields, null, valueFields)).isInstanceOf(IllegalStateException.class)
                .hasMessage(expectedMessage, TEST_ENRICHMENT_TYPE);
    }

}
