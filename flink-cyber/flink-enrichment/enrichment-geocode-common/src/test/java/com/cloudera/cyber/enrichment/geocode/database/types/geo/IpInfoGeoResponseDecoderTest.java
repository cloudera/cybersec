package com.cloudera.cyber.enrichment.geocode.database.types.geo;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IpInfoGeoResponseDecoderTest extends GeoDatabaseResponseDecoderTest {

    public IpInfoGeoResponseDecoderTest() {
        super(new IpInfoGeoResponseDecoder());
    }

    @Test
    public void testValidCity() {
        testValueRetrieval(IpInfoGeoResponseDecoder.CITY_KEY, "Boston", GeoDatabaseResponseDecoder::getCity);
    }

    @Test
    public void testValidPostalCode() {
        testValueRetrieval(IpInfoGeoResponseDecoder.POSTAL_CODE_KEY, "02855", GeoDatabaseResponseDecoder::getPostalCode);
    }

    @Test
    public void testValidLocationId() {
        testValueRetrieval(IpInfoGeoResponseDecoder.LOCATION_ID_KEY, "543", 543L, GeoDatabaseResponseDecoder::getLocationId);
    }

    @Test
    public void testEmptyLocationIdReturnsNull() {
        testValueRetrieval(IpInfoGeoResponseDecoder.LOCATION_ID_KEY, "", null, GeoDatabaseResponseDecoder::getLocationId);
    }

    @Test
    public void testNonNumericLocationIdReturnsNull() {
        testValueRetrieval(IpInfoGeoResponseDecoder.LOCATION_ID_KEY, "NOT A NUMBER", null, GeoDatabaseResponseDecoder::getLocationId);
    }
    @Test
    public void testValidDmaCode() {
        // DMA code is deprecated, always returns null
        assertNull(decoder.getDmaCode(Collections.emptyMap()));
    }

    @Test
    public void testValidState() {
        testValueRetrieval(IpInfoGeoResponseDecoder.STATE_KEY, "Massachusetts", GeoDatabaseResponseDecoder::getState);
    }

    @Test
    public void testValidCountry() {
        testValueRetrieval(IpInfoGeoResponseDecoder.COUNTRY_KEY, "US", GeoDatabaseResponseDecoder::getCountry);
    }

    @Test
    public void testValidLatitude() {
        testValueRetrieval(IpInfoGeoResponseDecoder.LATITUDE_KEY, 42.3555D, GeoDatabaseResponseDecoder::getLatitude);
    }

    @Test
    public void testValidLongitude() {
        testValueRetrieval(IpInfoGeoResponseDecoder.LONGITUDE_KEY, -71.0659, GeoDatabaseResponseDecoder::getLongitude);
    }

    private void testValueRetrieval(String key, Object value, BiFunction<GeoDatabaseResponseDecoder, Map<String, Object>, Object> testFunction) {
        testValueRetrieval(key, value, value, testFunction);
    }

    private void testValueRetrieval(String key, Object mapValue, Object expectedValue, BiFunction<GeoDatabaseResponseDecoder, Map<String, Object>, Object> testFunction) {
        Map<String, Object> response = new HashMap<>();
        response.put(key, mapValue);

        assertEquals(expectedValue, testFunction.apply(decoder, response));
    }
}
