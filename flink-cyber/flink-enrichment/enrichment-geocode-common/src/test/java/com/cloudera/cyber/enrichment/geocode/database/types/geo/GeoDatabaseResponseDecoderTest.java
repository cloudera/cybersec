package com.cloudera.cyber.enrichment.geocode.database.types.geo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Common tests for handling null and empty calls for geo methods.
 * Tests are called from child classes.  The tests in this class can't
 * be run independently.
 */
@Disabled
public class GeoDatabaseResponseDecoderTest {
    protected final GeoDatabaseResponseDecoder decoder;

    public GeoDatabaseResponseDecoderTest(GeoDatabaseResponseDecoder decoder) {
        this.decoder = decoder;
    }

    @Test
    public void testNullMapReturnsNull() {
        testNullReturns(null);
    }

    @Test
    public void testEmptyMapReturnsNull() {
        testNullReturns(Collections.emptyMap());
    }

    private void testNullReturns(Map<String, Object> response) {
        assertNull(decoder.getCity(response));
        assertNull(decoder.getPostalCode(response));
        assertNull(decoder.getLocationId(response));
        assertNull(decoder.getDmaCode(response));
        assertNull(decoder.getState(response));
        assertNull(decoder.getCountry(response));
        assertNull(decoder.getLatitude(response));
        assertNull(decoder.getLongitude(response));
    }
}
