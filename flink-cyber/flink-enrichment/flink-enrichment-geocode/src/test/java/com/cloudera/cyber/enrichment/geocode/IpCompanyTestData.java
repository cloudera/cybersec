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

package com.cloudera.cyber.enrichment.geocode;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloudera.cyber.enrichment.geocode.database.IpCompanyEnrichment.*;

/**
 * Test data for Company enrichment tests.
 * Uses invalid_maxmind_db.mmdb which is not a valid company database,
 * but allows testing error handling and quality message generation.
 */
public class IpCompanyTestData {
    
    /** Invalid database path for testing error handling */
    public static final String COMPANY_DATABASE_PATH = "./src/test/resources/geolite/invalid_maxmind_db.mmdb";
    
    /** IP address field name used in tests */
    public static final String IP_FIELD_NAME = "ip_dst_addr";
    
    /** Sample IP addresses for testing */
    public static final String CLOUDFLARE_IP = "1.1.1.1";
    public static final String LOCAL_IP = "127.0.0.1";
    public static final String UNKNOWN_HOST_IP = "example.com";
    
    /**
     * Returns the expected enrichment values for a given field name and IP.
     * For invalid databases, this returns an empty map since no enrichments are expected.
     */
    public static Map<String, String> getExpectedValues(String ipFieldName, String ipFieldValue) {
        // For an invalid database, no enrichments are expected
        return Collections.emptyMap();
    }
}
