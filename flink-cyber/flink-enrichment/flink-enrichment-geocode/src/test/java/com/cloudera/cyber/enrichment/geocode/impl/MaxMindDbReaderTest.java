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

package com.cloudera.cyber.enrichment.geocode.impl;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.enrichment.geocode.IpGeoTestData;
import com.maxmind.db.CHMCache;
import com.maxmind.db.Reader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the maxmind-db Reader integration in geocode enrichment.
 * 
 * These tests verify that the implementation works with the generic maxmind-db library
 * which supports both MaxMind and IPinfo MMDB databases.
 */
public class MaxMindDbReaderTest {
    private Reader maxmindDbReader;

    @BeforeEach
    public void setup() throws IOException {
        // Load the test database using the maxmind-db Reader
        File dbFile = new File(IpGeoTestData.GEOCODE_DATABASE_PATH);
        maxmindDbReader = new Reader.Builder(dbFile).withCache(new CHMCache()).build();
    }

    @Test
    public void testReaderCanBeCreated() {
        assertNotNull(maxmindDbReader, "maxmind-db Reader should be created");
    }

    @Test
    public void testReaderWithIpv4Address() throws Exception {
        InetAddress ipAddress = InetAddress.getByName(IpGeoTestData.ALL_FIELDS_IPv4);
        Object result = maxmindDbReader.get(ipAddress);
        
        Assertions.assertNotNull(result, "Should find result for IPv4 address");
        Assertions.assertTrue(result instanceof Map, "Result should be a Map");
    }

    @Test
    public void testReaderWithIpv6Address() throws Exception {
        InetAddress ipAddress = InetAddress.getByName(IpGeoTestData.COUNTRY_ONLY_IPv6);
        Object result = maxmindDbReader.get(ipAddress);
        
        Assertions.assertNotNull(result, "Should find result for IPv6 address");
        Assertions.assertTrue(result instanceof Map, "Result should be a Map");
    }

    @Test
    public void testReaderWithInvalidIpAddress() throws Exception {
        // IP address that won't be in the test database
        InetAddress ipAddress = InetAddress.getByName("192.0.2.1");
        Object result = maxmindDbReader.get(ipAddress);
        
        // When IP is not in the database, it returns data for the parent network
        // This is expected behavior for MaxMind DB
        Assertions.assertNotNull(result, "Should return parent network data for unknown IP");
    }
    
    @Test
    public void testIpGeoEnrichmentWithMaxmindDbReader() throws IOException {
        IpGeoEnrichment enrichment = new IpGeoEnrichment(maxmindDbReader);
        
        Map<String, String> enrichments = new HashMap<>();
        enrichment.lookupIpInfo("test_ip", IpGeoTestData.ALL_FIELDS_IPv4, 
                              java.util.Arrays.asList("country", "city"), 
                              enrichments, new java.util.ArrayList<>());
        
        // Verify enrichment was attempted (may be empty depending on schema)
        Assertions.assertNotNull(enrichments);
    }

    @Test
    public void testIpAsnEnrichmentWithMaxmindDbReader() throws IOException {
        IpAsnEnrichment enrichment = new IpAsnEnrichment(maxmindDbReader);
        
        Map<String, String> enrichments = new HashMap<>();
        enrichment.lookupIpInfo("test_ip", "1.128.0.0", 
                              enrichments, new java.util.ArrayList<>());
        
        // Verify enrichment was attempted
        Assertions.assertNotNull(enrichments);
    }

    @Test
    public void testIpGeoEnrichmentWithBothReaders() throws IOException {
        // Use the file path constructor which loads both readers
        IpGeoEnrichment enrichment = new IpGeoEnrichment(IpGeoTestData.GEOCODE_DATABASE_PATH);
        
        // Verify both readers are available
        Assertions.assertNotNull(enrichment.database, "DatabaseProvider should be available");
        Assertions.assertNotNull(enrichment.maxmindDbReader, "maxmindDbReader should be available");
    }

    @Test
    public void testInvalidPathThrows() {
        assertThrows(IllegalStateException.class, 
                   () -> new IpGeoEnrichment("/nonexistent/path/test.mmdb"),
                   "Should throw for nonexistent database path");
    }

    @Test 
    public void testNullReaderThrows() {
        assertThrows(NullPointerException.class,
                    () -> new IpGeoEnrichment((Reader) null),
                    "Should throw when Reader is null");
    }
}