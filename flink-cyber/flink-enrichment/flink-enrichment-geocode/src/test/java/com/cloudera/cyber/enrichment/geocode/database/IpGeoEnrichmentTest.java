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

package com.cloudera.cyber.enrichment.geocode.database;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.geocode.IpGeoTestData;
import com.cloudera.cyber.enrichment.geocode.database.types.GeoEnrichmentFields;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class IpGeoEnrichmentTest {
    private IpGeoEnrichment ipGeoEnrichment;
    private static final String TEST_ENRICHMENT_FIELD_NAME = "test_field";

    @BeforeEach
    public void createGeoEnrichment() {
        ipGeoEnrichment = new IpGeoEnrichment(IpGeoTestData.GEOCODE_DATABASE_PATH);
    }

    @Test
    public void throwsWithNullAsnDatabaseForNullPathDb() {
        assertThrows(IllegalArgumentException.class, () -> new IpGeoEnrichment(null), "Expected IllegalArgumentException");
    }

    @Test
    public void testNullCityState() {
        testGeoEnrichment(IpGeoTestData.COUNTRY_ONLY_IPv6);
    }

    @Test
    public void testAllFieldsPresent() {
        testGeoEnrichment(IpGeoTestData.ALL_FIELDS_IPv4);
    }

    @Test
    public void testHostIsNotIp() {
        testGeoEnrichment(IpGeoTestData.UNKNOWN_HOST_IP, DataQualityMessageLevel.INFO, String.format(IpGeoEnrichment.FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS, IpGeoTestData.UNKNOWN_HOST_IP), ipGeoEnrichment);
    }

    @Test
    public void testLocalIp() {
        // local ips are legitimate addresses but don't have geocode info
        testGeoEnrichment(IpGeoTestData.LOCAL_IP);
    }

    @Test
    public void testNullEnrichmentValue() {
        Map<String, String> emptyEnrichments = new HashMap<>();
        List<DataQualityMessage> emptyMessages = new ArrayList<>();

        ipGeoEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, null, GeoEnrichmentFields.values(), emptyEnrichments, emptyMessages);
        Assertions.assertTrue(emptyEnrichments.isEmpty());
        Assertions.assertTrue(emptyMessages.isEmpty());
    }

    private void testGeoEnrichment(String ipAddress) {
        testGeoEnrichment(ipAddress, null, null, ipGeoEnrichment);
    }

    private void testGeoEnrichment(String ipAddress, DataQualityMessageLevel level, String messageText, IpGeoEnrichment testIpGeoEnrichment) {

        List<DataQualityMessage> expectedQualityMessages = createExpectedDataQualityMessages(level, messageText);

        Map<String, String> expectedExtensions = new HashMap<>();
        com.cloudera.cyber.enrichment.geocode.IpGeoTestData.getExpectedEnrichmentValues(expectedExtensions, TEST_ENRICHMENT_FIELD_NAME, ipAddress);

        Map<String, String> actualExtensions = new HashMap<>();
        List<DataQualityMessage> actualQualityMessages = new ArrayList<>();
        testIpGeoEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, ipAddress, GeoEnrichmentFields.values(), actualExtensions, actualQualityMessages);
        Assertions.assertEquals(expectedExtensions, actualExtensions);
        Assertions.assertEquals(expectedQualityMessages, actualQualityMessages);
    }

    private List<DataQualityMessage> createExpectedDataQualityMessages(DataQualityMessageLevel level, String messageText) {
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
        if (messageText != null) {
            dataQualityMessages.add(DataQualityMessage.builder()
                    .level(level.name())
                    .feature(IpGeoEnrichment.GEOCODE_FEATURE)
                    .field(IpGeoEnrichmentTest.TEST_ENRICHMENT_FIELD_NAME)
                    .message(messageText).build());
        }
        return dataQualityMessages;
    }

}
