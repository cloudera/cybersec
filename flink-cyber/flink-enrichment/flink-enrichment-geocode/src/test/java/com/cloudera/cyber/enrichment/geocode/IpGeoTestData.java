package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.enrichment.geocode.impl.IpGeoEnrichment;

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
    public static final Map<String, Map<IpGeoEnrichment.GeoEnrichmentFields, Object>> EXPECTED_VALUES = createGeoExpectedValues();
    public static final String COUNTRY_ONLY_IPv6 = "2001:0218:0000:0000:0000:0000:0000:0000";
    public static final String ALL_FIELDS_IPv4 = "2.125.160.216";
    public static final String UNKNOWN_HOST_IP = "this.is.not.ip";
    public static final String LOCAL_IP = "10.0.0.1";

    public static Map<String, Map<IpGeoEnrichment.GeoEnrichmentFields, Object>> createGeoExpectedValues() {
        Map<String, Map<IpGeoEnrichment.GeoEnrichmentFields, Object>> expectedValues = new HashMap<>();
        expectedValues.put(COUNTRY_ONLY_IPv6,
                new HashMap<IpGeoEnrichment.GeoEnrichmentFields, Object>() {{
                    put(IpGeoEnrichment.GeoEnrichmentFields.COUNTRY, "JP");
                    put(IpGeoEnrichment.GeoEnrichmentFields.LATITUDE, 35.68536);
                    put(IpGeoEnrichment.GeoEnrichmentFields.LONGITUDE, 139.75309);
                }});
        expectedValues.put(ALL_FIELDS_IPv4,
                new HashMap<IpGeoEnrichment.GeoEnrichmentFields, Object>() {{
                    put(IpGeoEnrichment.GeoEnrichmentFields.COUNTRY, "GB");
                    put(IpGeoEnrichment.GeoEnrichmentFields.CITY, "Boxford");
                    put(IpGeoEnrichment.GeoEnrichmentFields.STATE, "West Berkshire");
                    put(IpGeoEnrichment.GeoEnrichmentFields.LATITUDE, 51.75);
                    put(IpGeoEnrichment.GeoEnrichmentFields.LONGITUDE, -1.25);
                }});

        return expectedValues;
    }

    public static  Map<IpGeoEnrichment.GeoEnrichmentFields, Object> getExpectedGeoEnrichments(String ipAddress) {
        return EXPECTED_VALUES.getOrDefault(ipAddress, Collections.emptyMap());
    }

    public static void getExpectedEnrichmentValues(Map<String, Object> expectedEnrichments, String enrichmentFieldName, String ipAddress) {
        Map<IpGeoEnrichment.GeoEnrichmentFields, Object> expectedGeos = getExpectedGeoEnrichments(ipAddress);
        expectedGeos.forEach((field, value) -> expectedEnrichments.put(String.join(".", enrichmentFieldName, GEOCODE_FEATURE, field.getSingularName()), value));
    }

    public static void getExpectedEnrichmentValues(Map<String, Object> expectedEnrichments, String enrichmentFieldName, List<Object> ipAddresses) {
        for(Object ipAddress : ipAddresses) {
            if (ipAddress instanceof String) {
                Map<IpGeoEnrichment.GeoEnrichmentFields, Object> expectedGeos = getExpectedGeoEnrichments((String) ipAddress);
                expectedGeos.forEach((field, value) -> {
                    if (value instanceof String && (field.equals(IpGeoEnrichment.GeoEnrichmentFields.COUNTRY) ||
                            field.equals(IpGeoEnrichment.GeoEnrichmentFields.CITY))) {
                        String qualifiedEnrichmentName = String.join(".", enrichmentFieldName, GEOCODE_FEATURE, field.getPluralName());
                        expectedEnrichments.putIfAbsent(qualifiedEnrichmentName, new HashSet<>());
                        //noinspection unchecked
                        ((Set<Object>) expectedEnrichments.get(qualifiedEnrichmentName)).add(value);
                    }
                });
            }
        }
    }

    public static void getExpectedEnrichmentValues(Map<String, Object> expectedEnrichments, String enrichmentFieldName, Object enrichmentValue) {
        if (enrichmentValue instanceof String) {
            getExpectedEnrichmentValues(expectedEnrichments, enrichmentFieldName, (String) enrichmentValue);
        } else {
            //noinspection unchecked
            getExpectedEnrichmentValues(expectedEnrichments, enrichmentFieldName, (List<Object>) enrichmentValue);
        }
    }
}
