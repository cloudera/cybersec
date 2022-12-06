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

import com.cloudera.cyber.enrichment.geocode.impl.types.GeoEnrichmentFields;
import com.google.common.collect.ImmutableMap;

import java.util.*;

import static com.cloudera.cyber.enrichment.geocode.IpGeoMap.GEOCODE_FEATURE;

/**
 * Small test database downloaded from the maxmind github:
 * https://github.com/maxmind/MaxMind-DB/blob/master/test-data/GeoIP2-City-Test.mmdb
 *
 * The json file that describes the IP ranges encoded in the database:
 * https://github.com/maxmind/MaxMind-DB/blob/master/source-data/GeoIP2-City-Test.json
 *
 * The article that describes how to use the test databases:
 * https://medium.com/@ivastly/how-to-use-test-versions-of-maxmind-geoip-databases-1a600fbd074c
 */
public class IpGeoTestData {
    public static final String GEOCODE_DATABASE_PATH = "./src/test/resources/geolite/GeoIP2-City-Test.mmdb";
    public static final Map<String, Map<GeoEnrichmentFields, String>> EXPECTED_VALUES = createGeoExpectedValues();
    public static final String COUNTRY_ONLY_IPv6 = "2001:0218:0000:0000:0000:0000:0000:0000";
    public static final String ALL_FIELDS_IPv4 = "2.125.160.216";
    public static final String UNKNOWN_HOST_IP = "this.is.not.ip";
    public static final String LOCAL_IP = "10.0.0.1";

    public static Map<String, Map<GeoEnrichmentFields, String>> createGeoExpectedValues() {
        Map<String, Map<GeoEnrichmentFields, String>> expectedValues = new HashMap<>();
        expectedValues.put(COUNTRY_ONLY_IPv6,
                ImmutableMap.of(
                    GeoEnrichmentFields.COUNTRY, "JP",
                    GeoEnrichmentFields.LATITUDE, "35.68536",
                    GeoEnrichmentFields.LONGITUDE, "139.75309"
                ));
        expectedValues.put(ALL_FIELDS_IPv4,
                ImmutableMap.of(
                    GeoEnrichmentFields.COUNTRY, "GB",
                    GeoEnrichmentFields.CITY, "Boxford",
                    GeoEnrichmentFields.STATE, "West Berkshire",
                    GeoEnrichmentFields.LATITUDE, "51.75",
                    GeoEnrichmentFields.LONGITUDE, "-1.25"
                ));

        return expectedValues;
    }

    public static  Map<GeoEnrichmentFields, String> getExpectedGeoEnrichments(String ipAddress) {
        return EXPECTED_VALUES.getOrDefault(ipAddress, Collections.emptyMap());
    }

    public static void getExpectedEnrichmentValues(Map<String, String> expectedEnrichments, String enrichmentFieldName, String ipAddress) {
        Map<GeoEnrichmentFields, String> expectedGeos = getExpectedGeoEnrichments(ipAddress);
        expectedGeos.forEach((field, value) -> expectedEnrichments.put(String.join(".", enrichmentFieldName, GEOCODE_FEATURE, field.getSingularName()), value));
    }

}
