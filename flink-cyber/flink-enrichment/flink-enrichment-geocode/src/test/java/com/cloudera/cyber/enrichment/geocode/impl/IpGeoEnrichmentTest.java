package com.cloudera.cyber.enrichment.geocode.impl;

import com.cloudera.cyber.enrichment.geocode.IpGeoTestData;
import com.maxmind.geoip2.DatabaseReader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;

public class IpGeoEnrichmentTest {
    private IpGeoEnrichment ipGeoEnrichment;

    @Before
    public void createGeoEnrichment() throws Exception {
        ipGeoEnrichment = new IpGeoEnrichment(new DatabaseReader.Builder(new File(IpGeoTestData.GEOCODE_DATABASE_PATH)).build());
    }

    @Test(expected=NullPointerException.class)
    public void throwsWithNullCityDatabase() {
        new IpGeoEnrichment(null);
    }

    @Test
    public void testNullCityState() throws Exception {
        testGeoEnrichment(IpGeoTestData.COUNTRY_ONLY_IPv6);
    }

    @Test
    public void testAllFieldsPresent() throws Exception {
        testGeoEnrichment(IpGeoTestData.ALL_FIELDS_IPv4);
    }

    @Test(expected = UnknownHostException.class)
    public void testUnknownHost() throws Exception {
        testGeoEnrichment(IpGeoTestData.UNKNOWN_HOST_IP);
    }

    @Test
    public void testLocalIp() throws Exception {
        // local ips are legitimate addresses but don't have geocode info
        testGeoEnrichment(IpGeoTestData.LOCAL_IP);
    }

    private void testGeoEnrichment(String ipAddress) throws Exception {
        Map<IpGeoEnrichment.GeoEnrichmentFields, Object> expectedValues =
                IpGeoTestData.getExpectedGeoEnrichments(ipAddress);
        Assert.assertEquals(expectedValues, ipGeoEnrichment.lookup(ipAddress, IpGeoEnrichment.GeoEnrichmentFields.values()));
    }

}
