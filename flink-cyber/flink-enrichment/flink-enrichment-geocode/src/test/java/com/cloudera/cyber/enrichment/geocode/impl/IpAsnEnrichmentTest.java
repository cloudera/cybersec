package com.cloudera.cyber.enrichment.geocode.impl;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.geocode.IpAsnTestData;
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

public class IpAsnEnrichmentTest {
    private IpAsnEnrichment ipAsnEnrichment;
    private static final String TEST_ENRICHMENT_FIELD_NAME = "test_field";

    @Before
    public void createAsnEnrichment()  {
        ipAsnEnrichment = new IpAsnEnrichment((IpAsnTestData.ASN_DATABASE_PATH));
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsWithNullAsnDatabaseForNullPathDb() {
        new IpAsnEnrichment((String) null);
    }

    @Test(expected = NullPointerException.class)
    public void throwsWithNullAsnDatabaseForNullDb() {
        new IpAsnEnrichment((DatabaseProvider) null);
    }

    @Test
    public void testIPv4AsnMapped() {
        testAsnEnrichment(IpAsnTestData.IP_WITH_NUMBER_AND_ORG);
    }

    @Test
    public void testIPv6AsnMapped() {
        testAsnEnrichment(IpAsnTestData.IP_V6_WITH_NUMBER_AND_ORG);
    }

    @Test
    public void testNoAsn() {
        testAsnEnrichment("1.1.1.1");
    }

   @Test
    public void testHostIsNotIp() {
        testAsnEnrichment(IpGeoTestData.UNKNOWN_HOST_IP, DataQualityMessageLevel.INFO, String.format(IpGeoEnrichment.FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS, IpGeoTestData.UNKNOWN_HOST_IP), ipAsnEnrichment);
    }

    @Test
    public void testLocalIp() {
        // local ips are legitimate addresses but don't have geocode info
        testAsnEnrichment(IpGeoTestData.LOCAL_IP);
    }

    @Test
    public void testNullEnrichmentValue() {
        Map<String, String> emptyEnrichments = new HashMap<>();
        List<DataQualityMessage> emptyMessages = new ArrayList<>();

        ipAsnEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, null, emptyEnrichments, emptyMessages);
        Assert.assertTrue(emptyEnrichments.isEmpty());
        Assert.assertTrue(emptyMessages.isEmpty());
    }


    @Test
    public void testMaxmindThrows() throws IOException, GeoIp2Exception {
        String testExceptionMessage = "this is a test message";
        DatabaseProvider throwingMaxmind = mock(DatabaseProvider.class);
        when(throwingMaxmind.tryAsn(any()))
                .thenThrow(new GeoIp2Exception(testExceptionMessage), new RuntimeException());

        // use non-local ip to test throwing path
        testAsnEnrichment("100.200.200.1", DataQualityMessageLevel.ERROR, String.format(IpAsnEnrichment.ASN_FAILED_MESSAGE, testExceptionMessage), new IpAsnEnrichment(throwingMaxmind));
    }


    private void testAsnEnrichment(String ipAddress) {
        testAsnEnrichment(ipAddress, null, null, ipAsnEnrichment);
    }

    private void testAsnEnrichment(String ipAddress, DataQualityMessageLevel level, String messageText, IpAsnEnrichment testAsnEnrichment) {

        List<DataQualityMessage> expectedQualityMessages = createExpectedDataQualityMessages(level, messageText);

        Map<String, String> expectedExtensions = IpAsnTestData.getExpectedValues(TEST_ENRICHMENT_FIELD_NAME, ipAddress);

        Map<String, String> actualExtensions = new HashMap<>();
        List<DataQualityMessage> actualQualityMessages = new ArrayList<>();
        testAsnEnrichment.lookup(TEST_ENRICHMENT_FIELD_NAME, ipAddress, actualExtensions, actualQualityMessages);
        Assert.assertEquals(expectedExtensions, actualExtensions);
        Assert.assertEquals(expectedQualityMessages, actualQualityMessages);
    }

    private List<DataQualityMessage> createExpectedDataQualityMessages(DataQualityMessageLevel level, String messageText) {
        List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
        if (messageText != null) {
            dataQualityMessages.add(DataQualityMessage.builder()
                    .level(level.name())
                    .feature(IpAsnEnrichment.ASN_FEATURE)
                    .field(TEST_ENRICHMENT_FIELD_NAME)
                    .message(messageText).build());
        }
        return dataQualityMessages;
    }

}
