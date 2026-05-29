package com.cloudera.cyber.enrichment.geocode.database.types.geo;

import com.cloudera.cyber.enrichment.geocode.database.types.TestResource;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.cloudera.cyber.enrichment.geocode.database.types.geo.MaxmindGeoResponseDecoder.LATITUDE_KEY;
import static com.cloudera.cyber.enrichment.geocode.database.types.geo.MaxmindGeoResponseDecoder.LOCATION_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GeoDatabaseTest {
    private static final String MAXMIND_GEO_MMDB = "geolite/GeoIP2-City-Test.mmdb";
    private static final TestResource resource = new TestResource();


    @Test
    public void testMaxmind() throws URISyntaxException, IOException {
        GeoDatabase database = new GeoDatabase(resource.getFilePath(MAXMIND_GEO_MMDB));
        assertNull(database.lookup(InetAddress.getByName("1.1.1.1")));
        assertNull(database.lookup(null));

        Map<String, Object> ipWithLocation = database.lookup(InetAddress.getByName("2.125.160.216"));
        double ipLatitude = 51.75;
        double ipLongitude = -1.25;
        assertEquals("GB", database.getCountry(ipWithLocation));
        assertEquals("Boxford", database.getCity(ipWithLocation));
        assertEquals(ipLatitude, database.getLatitude(ipWithLocation));
        assertEquals(ipLongitude, database.getLongitude(ipWithLocation));
        assertEquals(String.join(",", String.valueOf(ipLatitude), String.valueOf(ipLongitude)), database.getLocationPoint(ipWithLocation));
        assertNull(database.getDmaCode(ipWithLocation));
        assertEquals(2655045L, database.getLocationId(ipWithLocation));
        assertEquals("OX1", database.getPostalCode(ipWithLocation));
        assertEquals("West Berkshire", database.getState(ipWithLocation));

        Map<String, Object> ipv6 = database.lookup(InetAddress.getByName("2001:0218:0000:0000:0000:0000:0000:0000"));
        assertEquals("JP", database.getCountry(ipv6));
        assertNull(database.getCity(ipv6));
        assertEquals(35.68536D, database.getLatitude(ipv6));
        assertEquals(139.75309D, database.getLongitude(ipv6));
        assertNull(database.getDmaCode(ipv6));
        assertNull(database.getLocationId(ipv6));
        assertNull(database.getPostalCode(ipv6));
        assertNull(database.getState(ipv6));
    }

    @Test
    public void testLocationPointWithMissingLatOrLong() throws URISyntaxException {
        GeoDatabase database = new GeoDatabase(resource.getFilePath(MAXMIND_GEO_MMDB));

        assertNull(database.getLocationPoint(Collections.emptyMap()));

        Map<String, Object> location = new HashMap<>();
        location.put(LATITUDE_KEY, -37.5D);
        Map<String, Object> missingLongitude = new HashMap<>();
        missingLongitude.put(LOCATION_KEY, location);
        assertNull(database.getLocationPoint(missingLongitude));
    }
}
