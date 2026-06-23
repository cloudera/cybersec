package com.cloudera.cyber.enrichment.geocode.database;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.geocode.IpGeoTestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for IpCompanyEnrichment.
 */
public class IpCompanyEnrichmentTest {

    private static final String TEST_ENRICHMENT_FIELD_NAME = "test_field";
    private static final String INVALID_DATABASE_PATH = "./src/test/resources/geolite/invalid_maxmind_db.mmdb";
    
    private IpCompanyEnrichment ipCompanyEnrichment;

    @BeforeEach
    void createCompanyEnrichment() {
        ipCompanyEnrichment = new IpCompanyEnrichment(INVALID_DATABASE_PATH);
    }

    @AfterEach
    void cleanup() throws Exception {
        if (ipCompanyEnrichment != null) {
            ipCompanyEnrichment.close();
        }
    }

    @Test
    void throwsWithInvalidDatabase() {
        // The invalid_maxmind_db.mmdb is not a valid company database, so lookup should fail
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> messages = new ArrayList<>();
        
        // Lookup should add a quality message for the error
        ipCompanyEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, IpGeoTestData.CLOUDFLARE_IP, extensions, messages);
        
        // Should have a quality message due to the database error
        Assertions.assertFalse(messages.isEmpty());
        DataQualityMessage errorMessage = messages.get(0);
        Assertions.assertEquals(DataQualityMessageLevel.ERROR.name(), errorMessage.getLevel());
        Assertions.assertTrue(errorMessage.getMessage().contains("Company lookup failed"));
    }

    @Test
    void testHostIsNotIpAddsInfoMessage() {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> messages = new ArrayList<>();
        
        ipCompanyEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, IpGeoTestData.UNKNOWN_HOST_IP, extensions, messages);
        
        Assertions.assertEquals(1, messages.size());
        DataQualityMessage infoMessage = messages.get(0);
        Assertions.assertEquals(DataQualityMessageLevel.INFO.name(), infoMessage.getLevel());
        Assertions.assertEquals(IpGeoEnrichment.FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS, infoMessage.getMessage());
        Assertions.assertEquals(IpCompanyEnrichment.COMPANY_FEATURE, infoMessage.getFeature());
        Assertions.assertEquals(TEST_ENRICHMENT_FIELD_NAME, infoMessage.getField());
    }

    @Test
    void testNullEnrichmentValueDoesNothing() {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> messages = new ArrayList<>();
        
        ipCompanyEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, null, extensions, messages);
        
        Assertions.assertTrue(extensions.isEmpty());
        Assertions.assertTrue(messages.isEmpty());
    }

    @Test
    void testEmptyExtensionsAndMessagesAreNotModifiedOnNullValue() {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> messages = new ArrayList<>();
        
        ipCompanyEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, null, extensions, messages);
        
        Assertions.assertTrue(extensions.isEmpty());
        Assertions.assertTrue(messages.isEmpty());
    }

    @Test
    void testCollectionOfIpsProcessedIndividually() {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> messages = new ArrayList<>();
        List<String> ips = Arrays.asList(IpGeoTestData.UNKNOWN_HOST_IP, IpGeoTestData.LOCAL_IP);
        
        ipCompanyEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, ips, extensions, messages);
        
        // Each IP should generate its own quality message
        Assertions.assertEquals(2, messages.size());
    }

    @Test
    void testLocalIpProcessedWithoutEnrichment() {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> messages = new ArrayList<>();
        
        ipCompanyEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, IpGeoTestData.LOCAL_IP, extensions, messages);
        
        // Local IPs don't have enrichment data, but they're valid IPs
        Assertions.assertTrue(extensions.isEmpty());
        // No quality messages for local IPs
        Assertions.assertTrue(messages.isEmpty());
    }

    @Test
    void testBiFunctionLookupWithCollection() {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> messages = new ArrayList<>();
        List<String> ips = Collections.singletonList(IpGeoTestData.UNKNOWN_HOST_IP);
        
        ipCompanyEnrichment.lookup(
            (fieldName, feature) -> new com.cloudera.cyber.enrichment.SingleValueEnrichment(fieldName, feature),
            TEST_ENRICHMENT_FIELD_NAME,
            ips,
            extensions,
            messages
        );
        
        Assertions.assertEquals(1, messages.size());
    }

    @Test
    void testBiFunctionLookupWithSingleIp() {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> messages = new ArrayList<>();
        
        ipCompanyEnrichment.lookup(
            (fieldName, feature) -> new com.cloudera.cyber.enrichment.SingleValueEnrichment(fieldName, feature),
            TEST_ENRICHMENT_FIELD_NAME,
            IpGeoTestData.UNKNOWN_HOST_IP,
            extensions,
            messages
        );
        
        Assertions.assertEquals(1, messages.size());
    }

    @Test
    void testBiFunctionLookupWithNullIp() {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> messages = new ArrayList<>();
        
        ipCompanyEnrichment.lookup(
            (fieldName, feature) -> new com.cloudera.cyber.enrichment.SingleValueEnrichment(fieldName, feature),
            TEST_ENRICHMENT_FIELD_NAME,
            null,
            extensions,
            messages
        );
        
        Assertions.assertTrue(extensions.isEmpty());
        Assertions.assertTrue(messages.isEmpty());
    }

    @Test
    void testEnrichmentAddsCompanyFeatureToMessages() {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> messages = new ArrayList<>();
        
        ipCompanyEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, IpGeoTestData.UNKNOWN_HOST_IP, extensions, messages);
        
        Assertions.assertFalse(messages.isEmpty());
        Assertions.assertEquals(IpCompanyEnrichment.COMPANY_FEATURE, messages.get(0).getFeature());
    }
}
