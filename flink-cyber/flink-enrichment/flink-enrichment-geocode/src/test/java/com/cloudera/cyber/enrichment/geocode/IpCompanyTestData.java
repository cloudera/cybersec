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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloudera.cyber.enrichment.geocode.database.IpCompanyEnrichment.COMPANY_FEATURE;
import static com.cloudera.cyber.enrichment.geocode.database.IpCompanyEnrichment.COMPANY_NAME_PREFIX;
import static com.cloudera.cyber.enrichment.geocode.database.IpCompanyEnrichment.ASN_NUMBER_PREFIX;
import static com.cloudera.cyber.enrichment.geocode.database.IpCompanyEnrichment.ASN_ORG_PREFIX;
import static com.cloudera.cyber.enrichment.geocode.database.IpCompanyEnrichment.COMPANY_MASK_PREFIX;

/**
 * Test data for Company enrichment tests.
 * Uses invalid_maxmind_db.mmdb which is not a valid company database,
 * but allows testing error handling and quality message generation.
 */
public class IpCompanyTestData {
    
    /** Invalid database path for testing error handling */
    public static final String COMPANY_DATABASE_PATH = "./src/test/resources/ipinfo/ip_company_sample.mmdb";
    
    /** IP address field name used in tests */
    public static final String IP_FIELD_NAME = "ip_dst_addr";
    
    public static final String INVALID_DATABASE_PATH = "./src/test/resources/geolite/invalid_maxmind_db.mmdb";

    /**
     * IP in sample database with all information present - company name, ASN Number, and ASN org.
     */
    public static final String IP_WITH_NUMBER_AND_ORG = "1.10.10.0";
    /**
     * IP in the sample database with company name only.
     */
    public static final String IP_WITH_NO_INFO = "1.25.0.0";
    /**
     * IP not in sample database.
     */
    public static final String IP_COMPANY_ONLY = "1.0.17.0";

    public static final String LOCAL_IP = "127.0.0.1";
    public static final String UNKNOWN_HOST_IP = "example.com";

    public static final Map<String, Map<String, String>> ipToCompanyEnrichments = ImmutableMap.of(
            IP_WITH_NUMBER_AND_ORG, ImmutableMap.of(
                    COMPANY_NAME_PREFIX, "National Internet Exchange of India",
                    ASN_NUMBER_PREFIX, "148000",
                    ASN_ORG_PREFIX, "BHARAT PUBLIC DNS (1.10.10.10)",
                    COMPANY_MASK_PREFIX, "1.10.10.0/24"),
            IP_COMPANY_ONLY, ImmutableMap.of(
                    COMPANY_NAME_PREFIX, "i2ts inc.",
                    COMPANY_MASK_PREFIX, "1.0.17.0/24"),
            IP_WITH_NO_INFO, Collections.emptyMap(),
            "1.1.1.1", ImmutableMap.of(
                    COMPANY_NAME_PREFIX, "APNIC and Cloudflare DNS Resolver project",
                    ASN_NUMBER_PREFIX, "13335",
                    ASN_ORG_PREFIX, "Cloudflare, Inc.",
                    COMPANY_MASK_PREFIX, "1.1.1.0/24")
            );

    public static Map<String, String> getExpectedValues(String ipFieldName, String ipFieldValue) {
        Map<String, String> companyValues = ipToCompanyEnrichments.get(ipFieldValue);
        if (companyValues != null) {
            return companyValues.entrySet().stream().collect(Collectors.toMap(e -> Joiner.on(".").join(ipFieldName, COMPANY_FEATURE, e.getKey()), Map.Entry::getValue));
        } else {
            return Collections.emptyMap();
        }
    }

    public static Map<String, String> getExpectedExtension(Map<String, String> inputFields, List<String> companyEnrichedFields) {
        Map<String, String> expectedExtensions = new HashMap<>(inputFields);
        inputFields.forEach((fieldName, fieldValue) -> {
            if (companyEnrichedFields.contains(fieldName)) {
                expectedExtensions.putAll(IpCompanyTestData.getExpectedValues(fieldName, fieldValue));
            }
        });
        return expectedExtensions;
    }
}
