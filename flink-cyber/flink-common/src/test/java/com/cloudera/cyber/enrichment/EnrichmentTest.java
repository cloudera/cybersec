package com.cloudera.cyber.enrichment;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnrichmentTest {
    private static final List<String> TEST_FIELDS = List.of("field1", "field2");
    private static final String TEST_FEATURE_NAME = "enrich_feature";
    @Test
    public void testGetEnrichmentPrefixForField() {
        String fieldName = "my_field";
        assertEquals(getExpectedEnrichmentPrefix(fieldName),
                Enrichment.getEnrichmentPrefixForField(fieldName, TEST_FEATURE_NAME));
    }

    @Test
    public void testGetEnrichmentPrefixForFields() {
        List<String> expectedPrefixes = TEST_FIELDS.stream().map(EnrichmentTest::getExpectedEnrichmentPrefix).toList();
        assertEquals(expectedPrefixes, Enrichment.getEnrichmentPrefixesForFields(TEST_FIELDS, TEST_FEATURE_NAME));

        assertEquals(Collections.emptyList(), Enrichment.getEnrichmentPrefixesForFields(Collections.emptyList(), TEST_FEATURE_NAME));
    }

    @Test
    public void testGetEnrichmentName() {
        String field = "field";
        String feature = "feature";
        String enrichment = "enrichment";
        SingleValueEnrichment singleValueEnrichment = new SingleValueEnrichment(field, feature);
        assertEquals(String.format("%s.%s.%s", field, feature, enrichment), singleValueEnrichment.getName(enrichment));

        MetronGeoEnrichment geoEnrichment = new MetronGeoEnrichment(field, feature);
        assertEquals(String.format("%s.%s", field, enrichment), geoEnrichment.getName(enrichment));

    }

    private static String getExpectedEnrichmentPrefix(String fieldName) {
        return String.format("%s.%s.", fieldName, TEST_FEATURE_NAME);
    }
}
