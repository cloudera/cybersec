package com.cloudera.cyber.enrichment.geocode.impl;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.geocode.IpGeoTestData;
import com.maxmind.geoip2.DatabaseProvider;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IpGeoEnrichmentTest {
    private IpGeoEnrichment ipGeoEnrichment;
    private static final String TEST_ENRICHMENT_FIELD_NAME = "test_field";

    @Before
    public void createGeoEnrichment() throws Exception {
        ipGeoEnrichment = new IpGeoEnrichment(new DatabaseReader.Builder(new File(IpGeoTestData.GEOCODE_DATABASE_PATH)).build());
    }

    @Test(expected = NullPointerException.class)
    public void throwsWithNullCityDatabase() {
        new IpGeoEnrichment(null);
    }

    @Test
    public void testNullCityState() {
        testGeoEnrichment(IpGeoTestData.COUNTRY_ONLY_IPv6);
    }

    @Test
    public void testAllFieldsPresent() {
        testGeoEnrichment(IpGeoTestData.ALL_FIELDS_IPv4);
    }

    @Test
    public void testHostIsNotIp() {
        testGeoEnrichment(IpGeoTestData.UNKNOWN_HOST_IP, DataQualityMessageLevel.INFO, String.format(IpGeoEnrichment.FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS, IpGeoTestData.UNKNOWN_HOST_IP), ipGeoEnrichment);
    }

    @Test
    public void testLocalIp() {
        // local ips are legitimate addresses but don't have geocode info
        testGeoEnrichment(IpGeoTestData.LOCAL_IP);
    }

    @Test
    public void testNullEnrichmentValue() {
        Map<String, String> emptyEnrichments = new HashMap<>();
        List<DataQualityMessage> emptyMessages = new ArrayList<>();

        ipGeoEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, null, IpGeoEnrichment.GeoEnrichmentFields.values(), emptyEnrichments, emptyMessages);
        Assert.assertTrue(emptyEnrichments.isEmpty());
        Assert.assertTrue(emptyMessages.isEmpty());
    }

    @Test
    public void testMaxmindThrows() throws IOException, GeoIp2Exception {
        String testExceptionMessage = "this is a test message";
        DatabaseProvider throwingMaxmind = mock(DatabaseProvider.class);
        when(throwingMaxmind.tryCity(any()))
                .thenThrow(new GeoIp2Exception(testExceptionMessage), new RuntimeException());

        // use non-local ip to test throwing path
        testGeoEnrichment("100.200.200.1", DataQualityMessageLevel.ERROR, String.format(IpGeoEnrichment.GEOCODE_FAILED_MESSAGE, testExceptionMessage), new IpGeoEnrichment(throwingMaxmind));
    }

    private void testGeoEnrichment(String ipAddress) {
        testGeoEnrichment(ipAddress, null, null, ipGeoEnrichment);
    }

    private void testGeoEnrichment(String ipAddress, DataQualityMessageLevel level, String messageText, IpGeoEnrichment testIpGeoEnrichment) {

        List<DataQualityMessage> expectedQualityMessages = createExpectedDataQualityMessages(level, messageText);

        Map<String, String> expectedExtensions = new HashMap<>();
        com.cloudera.cyber.enrichment.geocode.IpGeoTestData.getExpectedEnrichmentValues(expectedExtensions, TEST_ENRICHMENT_FIELD_NAME, ipAddress, Arrays.asList(IpGeoEnrichment.GeoEnrichmentFields.values()));

        Map<String, String> actualExtensions = new HashMap<>();
        List<DataQualityMessage> actualQualityMessages = new ArrayList<>();
        testIpGeoEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, ipAddress, IpGeoEnrichment.GeoEnrichmentFields.values(), actualExtensions, actualQualityMessages);
        Assert.assertEquals(expectedExtensions, actualExtensions);
        Assert.assertEquals(expectedQualityMessages, actualQualityMessages);
    }

    private List<DataQualityMessage> createExpectedDataQualityMessages(DataQualityMessageLevel level, String messageText) {
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
        if (messageText != null) {
            dataQualityMessages.add(DataQualityMessage.builder()
                    .level(level.name())
                    .feature(IpGeoEnrichment.GEOCODE_FEATURE)
                    .field(IpGeoEnrichmentTest.TEST_ENRICHMENT_FIELD_NAME)
                    .message(messageText).build());
        }
        return dataQualityMessages;
    }

}
