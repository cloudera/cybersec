package com.cloudera.cyber.enrichment;


import org.apache.flink.api.java.utils.ParameterTool;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.cloudera.cyber.enrichment.EnrichmentConfiguration.PARAMS_ENABLE_ASN;
import static com.cloudera.cyber.enrichment.EnrichmentConfiguration.PARAMS_ENABLE_CIDR;
import static com.cloudera.cyber.enrichment.EnrichmentConfiguration.PARAMS_ENABLE_COMPANY;
import static com.cloudera.cyber.enrichment.EnrichmentConfiguration.PARAMS_ENABLE_GEO;
import static com.cloudera.cyber.enrichment.EnrichmentConfiguration.PARAMS_LOOKUPS_CONFIG_FILE;
import static com.cloudera.cyber.enrichment.EnrichmentConfiguration.PARAM_ASN_FIELDS;
import static com.cloudera.cyber.enrichment.EnrichmentConfiguration.PARAM_CIDR_IP_FIELDS;
import static com.cloudera.cyber.enrichment.EnrichmentConfiguration.PARAM_COMPANY_FIELDS;
import static com.cloudera.cyber.enrichment.EnrichmentConfiguration.PARAM_GEO_FIELDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnrichmentConfigurationTest {

    @Test
    public void testGetGeoEnrichmentFieldPrefixes() {
        testGetGeoEnrichmentFieldPrefix(Map.of(PARAMS_ENABLE_GEO, "true",
                PARAM_GEO_FIELDS, "field1,field2,field3",
                PARAMS_ENABLE_ASN, "false",
                PARAMS_ENABLE_COMPANY, "false"), List.of("field1.geo.", "field2.geo.", "field3.geo."));

        testGetGeoEnrichmentFieldPrefix(Map.of(PARAM_GEO_FIELDS, "field1,field2,field3",
                PARAMS_ENABLE_ASN, "false",
                PARAMS_ENABLE_COMPANY, "false"), List.of("field1.geo.", "field2.geo.", "field3.geo."));

        testGetGeoEnrichmentFieldPrefix(Map.of(PARAMS_ENABLE_GEO, "false",
                PARAM_ASN_FIELDS, "field1,field2,field3",
                PARAMS_ENABLE_ASN, "true",
                PARAMS_ENABLE_COMPANY, "false"), List.of("field1.asn.", "field2.asn.", "field3.asn."));

        testGetGeoEnrichmentFieldPrefix(Map.of(PARAMS_ENABLE_GEO, "false",
                PARAM_ASN_FIELDS, "field1,field2,field3",
                PARAMS_ENABLE_COMPANY, "false"), List.of("field1.asn.", "field2.asn.", "field3.asn."));


        testGetGeoEnrichmentFieldPrefix(Map.of(PARAMS_ENABLE_GEO, "false",
                PARAM_COMPANY_FIELDS, "field1,field2,field3",
                PARAMS_ENABLE_ASN, "false",
                PARAMS_ENABLE_COMPANY, "true"), List.of("field1.company.", "field2.company.", "field3.company."));

        testGetGeoEnrichmentFieldPrefix(Map.of(PARAMS_ENABLE_GEO, "false",
                PARAM_COMPANY_FIELDS, "field1,field2,field3",
                PARAMS_ENABLE_ASN, "false"), List.of("field1.company.", "field2.company.", "field3.company."));
    }

    private void testGetGeoEnrichmentFieldPrefix(Map<String, String> paramMap, List<String> expectedPrefixes) {
        ParameterTool params = ParameterTool.fromMap(paramMap);
        assertEquals(expectedPrefixes, EnrichmentConfiguration.getGeoEnrichmentFieldPrefixes(params));
    }

    @Test
    public void testGetEnrichmentFieldPrefixes() throws IOException {
        Map<String, String> params = createDefaultMap();
        addFieldParams(params, PARAMS_ENABLE_CIDR, PARAM_CIDR_IP_FIELDS, "field1,field2");
        assertEquals(List.of("field1.region.", "field2.region."), EnrichmentConfiguration.getEnrichmentFieldPrefixes(ParameterTool.fromMap(params)));

        params = createDefaultMap();
        addFieldParams(params, PARAMS_ENABLE_CIDR, PARAM_CIDR_IP_FIELDS, null);
        assertEquals(Collections.emptyList(), EnrichmentConfiguration.getEnrichmentFieldPrefixes(ParameterTool.fromMap(params)));

        params = createDefaultMap();
        addFieldParams(params, PARAMS_ENABLE_GEO, PARAM_GEO_FIELDS, "field1,field3");
        addFieldParams(params, PARAMS_ENABLE_CIDR, PARAM_CIDR_IP_FIELDS, "field1,field2");
        assertEquals(List.of("field1.geo.", "field3.geo.", "field1.region.", "field2.region."), EnrichmentConfiguration.getEnrichmentFieldPrefixes(ParameterTool.fromMap(params)));
    }

    private Map<String, String> createDefaultMap() {
        Map<String, String> params = new HashMap<>();

        params.put(PARAMS_ENABLE_GEO, "false");
        params.put(PARAMS_ENABLE_COMPANY, "false");
        params.put(PARAMS_ENABLE_ASN, "false");
        params.put(PARAMS_ENABLE_CIDR, "false");

        return params;
    }

    private void addFieldParams(Map<String, String> params, String enabledKey, String fieldKey, String cidrFields) {
        if (cidrFields != null) {
            params.put(enabledKey, "true");
            params.put(fieldKey, cidrFields);
        } else {
            params.put(enabledKey, "false");
        }
    }

    @Test
    public void testGetSourceToEnrichmentPrefixMap() throws IOException, URISyntaxException {
        URL configUrl = Objects.requireNonNull(getClass().getClassLoader().getResource("enrichments-lookups.json"));
        String configFile = new File(configUrl.toURI()).getAbsolutePath();
        Map<String, String> paramMap = Map.of(PARAMS_LOOKUPS_CONFIG_FILE, configFile);
        Map<String, Set<String>> sourceToEnrichmentNames = EnrichmentConfiguration.getSourceEnrichmentFieldPrefixes(ParameterTool.fromMap(paramMap));

        Map<String, Set<String>> expectedSourceToEnrichmentFieldPrefixes = Map.of("squid",
                Set.of("domain_no_subdomains.domain_category.", "domain_no_subdomains.majestic_million.", "domain.malicious_domain."));
        assertEquals(expectedSourceToEnrichmentFieldPrefixes, sourceToEnrichmentNames);
    }

}
