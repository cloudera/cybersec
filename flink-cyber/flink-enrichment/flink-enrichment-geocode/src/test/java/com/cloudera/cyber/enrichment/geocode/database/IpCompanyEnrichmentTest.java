package com.cloudera.cyber.enrichment.geocode.database;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.SingleValueEnrichment;
import com.cloudera.cyber.enrichment.geocode.IpCompanyTestData;
import com.cloudera.cyber.enrichment.geocode.IpGeoTestData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloudera.cyber.enrichment.geocode.IpCompanyTestData.COMPANY_DATABASE_PATH;
import static com.cloudera.cyber.enrichment.geocode.IpCompanyTestData.INVALID_DATABASE_PATH;
import static com.cloudera.cyber.enrichment.geocode.IpCompanyTestData.IP_WITH_NUMBER_AND_ORG;

/**
 * Unit tests for IpCompanyEnrichment.
 */
public class IpCompanyEnrichmentTest {

    private static final String TEST_ENRICHMENT_FIELD_NAME = "test_field";
    private IpCompanyEnrichment ipCompanyEnrichment;

    @BeforeEach
    void createCompanyEnrichment() {
        ipCompanyEnrichment = new IpCompanyEnrichment(COMPANY_DATABASE_PATH);
    }

    @AfterEach
    void cleanup() throws Exception {
        if (ipCompanyEnrichment != null) {
            ipCompanyEnrichment.close();
        }
    }

    @Test
    void testSuccessfulLookup() {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();

        IpCompanyEnrichment ipCompanyEnrichment = new IpCompanyEnrichment(COMPANY_DATABASE_PATH);
        ipCompanyEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, IP_WITH_NUMBER_AND_ORG, extensions, dataQualityMessages);

        Assertions.assertTrue(dataQualityMessages.isEmpty());
        Assertions.assertEquals(IpCompanyTestData.getExpectedValues(TEST_ENRICHMENT_FIELD_NAME, IP_WITH_NUMBER_AND_ORG), extensions);
    }

    @Test
    void testLookupOnClosedDatabase() throws IOException {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
        IpCompanyEnrichment ipCompanyEnrichment = new IpCompanyEnrichment(COMPANY_DATABASE_PATH);
        ipCompanyEnrichment.close();
        ipCompanyEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, IP_WITH_NUMBER_AND_ORG, extensions, dataQualityMessages);
        Assertions.assertFalse(dataQualityMessages.isEmpty());
    }

    @Test
    void throwsWithInvalidDatabase() {
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,  () -> new IpCompanyEnrichment(INVALID_DATABASE_PATH));
        Assertions.assertTrue(exception.getMessage().contains(INVALID_DATABASE_PATH));
        Assertions.assertTrue(exception.getMessage().contains("Could not read geocode database"));
    }

    @Test
    void testHostIsNotIpAddsInfoMessage() {
        Map<String, String> extensions = new HashMap<>();
        List<DataQualityMessage> messages = new ArrayList<>();
        
        ipCompanyEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, IpGeoTestData.UNKNOWN_HOST_IP, extensions, messages);
        
        Assertions.assertEquals(1, messages.size());
        DataQualityMessage infoMessage = messages.get(0);
        Assertions.assertEquals(DataQualityMessageLevel.INFO.name(), infoMessage.getLevel());
        Assertions.assertEquals(String.format(IpGeoEnrichment.FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS, IpGeoTestData.UNKNOWN_HOST_IP), infoMessage.getMessage());
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
        Assertions.assertEquals(1, messages.size());
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
                SingleValueEnrichment::new,
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
                SingleValueEnrichment::new,
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
                SingleValueEnrichment::new,
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
