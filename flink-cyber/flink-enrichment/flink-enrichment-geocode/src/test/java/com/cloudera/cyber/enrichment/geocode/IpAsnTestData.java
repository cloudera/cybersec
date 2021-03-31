package com.cloudera.cyber.enrichment.geocode;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloudera.cyber.enrichment.geocode.impl.IpAsnEnrichment.*;

/**
 * Small test database downloaded from the maxmind github:
 * https://github.com/maxmind/MaxMind-DB/blob/main/test-data/GeoLite2-ASN-Test.mmdb
 *
 * The json file that describes the IP ranges encoded in the database:
 * https://github.com/maxmind/MaxMind-DB/blob/main/source-data/GeoLite2-ASN-Test.json
 *
 * The article that describes how to use the test databases:
 * https://medium.com/@ivastly/how-to-use-test-versions-of-maxmind-geoip-databases-1a600fbd074c
 */

public class IpAsnTestData {
    public static final String ASN_DATABASE_PATH = "./src/test/resources/geolite/GeoLite2-ASN-Test.mmdb";

    public static final String IP_WITH_NUMBER_AND_ORG = "1.128.0.0";
    public static final String IP_V6_WITH_NUMBER_AND_ORG = "2001:1700:0000:0000:0000:0000:0000:0000";
    public static final String IP_NOT_FOUND = "1.1.1.1";

    public static final Map<String, Map<String, String>> ipToAsnEnrichments = ImmutableMap.of(
        IP_WITH_NUMBER_AND_ORG, ImmutableMap.of(ASN_NUMBER_PREFIX, "1221",
                                         ASN_ORG_PREFIX, "Telstra Pty Ltd",
                                         ASN_MASK_PREFIX, "1.128.0.0/11"),
        IP_V6_WITH_NUMBER_AND_ORG, ImmutableMap.of(ASN_NUMBER_PREFIX, "6730",
                ASN_ORG_PREFIX, "Sunrise Communications AG",
                ASN_MASK_PREFIX, "2001:1700:0:0:0:0:0:0/27"),
        IP_NOT_FOUND, Collections.emptyMap());

    public static Map<String, String> getExpectedValues(String ipFieldName, String ipFieldValue) {
        Map<String, String> asnValues = ipToAsnEnrichments.get(ipFieldValue);
        if (asnValues != null) {
            return asnValues.entrySet().stream().collect(Collectors.toMap(e -> Joiner.on(".").join(ipFieldName, ASN_FEATURE,e.getKey()), Map.Entry::getValue));
        } else {
            return Collections.emptyMap();
        }
    }
}
