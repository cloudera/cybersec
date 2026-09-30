package com.cloudera.cyber.enrichment;

import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import org.apache.flink.api.java.utils.ParameterTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class EnrichmentConfiguration {

    public static final String PARAMS_ENABLE_GEO = "geo.enabled";
    public static final String PARAMS_ENABLE_ASN = "asn.enabled";
    public static final String PARAMS_ENABLE_COMPANY = "company.enabled";

    public static final String PARAM_GEO_FIELDS = "geo.ip_fields";
    public static final String PARAM_ASN_FIELDS = "asn.ip_fields";
    public static final String PARAM_COMPANY_FIELDS = "company.ip_fields";
    public static final String PARAMS_ENABLE_CIDR = "cidr.enabled";
    public static final String PARAMS_LOOKUPS_CONFIG_FILE = "lookups.config.file";
    public static final String PARAM_CIDR_IP_FIELDS = "cidr.ip_fields";

    public static List<String> getEnrichmentFieldPrefixes(ParameterTool params) throws IOException {
        // get the geocoding job prefixes
        List<String> enrichmentPrefixes =getGeoEnrichmentFieldPrefixes(params);

        // get the cidr job prefixes
        enrichmentPrefixes.addAll(Enrichment.getEnrichmentPrefixesForFields(getCidrEnrichmentFields(params), EnrichmentFeature.IP_REGION_CIDR_FEATURE_NAME));

        return enrichmentPrefixes;
    }

    public static Map<String, Set<String>> getSourceEnrichmentFieldPrefixes(ParameterTool params) throws IOException {
        // get the lookup enrichment prefixes
        List<EnrichmentConfig> lookupEnrichmentConfig = ConfigUtils.allConfigs(Files.readAllBytes(Paths.get(params.getRequired(PARAMS_LOOKUPS_CONFIG_FILE))));
        return ConfigUtils.enrichmentFieldPrefixes(lookupEnrichmentConfig);
    }

    public static List<String> getGeoEnrichmentFieldPrefixes(ParameterTool params) {
        List<String> prefixes = new ArrayList<>(Enrichment.getEnrichmentPrefixesForFields(getFieldsForGeoEnrichment(params, PARAMS_ENABLE_GEO, PARAM_GEO_FIELDS), EnrichmentFeature.GEOCODE_FEATURE));
        prefixes.addAll(Enrichment.getEnrichmentPrefixesForFields(getFieldsForGeoEnrichment(params, PARAMS_ENABLE_ASN, PARAM_ASN_FIELDS), EnrichmentFeature.ASN_FEATURE));
        prefixes.addAll(Enrichment.getEnrichmentPrefixesForFields(getFieldsForGeoEnrichment(params, PARAMS_ENABLE_COMPANY, PARAM_COMPANY_FIELDS), EnrichmentFeature.COMPANY_FEATURE));
        return prefixes;
    }

    public static List<String> getFieldsForGeoEnrichment(ParameterTool params, String enabledParamName, String enrichedFieldsParamName) {
        boolean enabled = params.getBoolean(enabledParamName, true);
        if (enabled) {
            return Arrays.asList(params.getRequired(enrichedFieldsParamName).split(","));
        } else {
            return Collections.emptyList();
        }
    }

    public static List<String> getCidrEnrichmentFields(ParameterTool params) {
        boolean cidrEnabled = params.getBoolean(PARAMS_ENABLE_CIDR, false);
        if (cidrEnabled) {
            return Arrays.asList(params.getRequired(PARAM_CIDR_IP_FIELDS).split(","));
        } else {
            return Collections.emptyList();
        }
    }
}
