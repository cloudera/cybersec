package com.cloudera.cyber.enrichment.geocode.database.types.asn;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MaxmindAsnResponseDecoderTest extends AsnDatabaseResponseBaseTest {
    private final MaxmindAsnResponseDecoder decoder = new MaxmindAsnResponseDecoder();

    public MaxmindAsnResponseDecoderTest() {
        super(new MaxmindAsnResponseDecoder());
    }

    @Test
    public void testValidAsnNumber() {
        Map<String, Object> mapWithAsn = new HashMap<>();
        long expectedAsn = 500L;
        mapWithAsn.put(MaxmindAsnResponseDecoder.AS_NUM_KEYWORD, expectedAsn);
        assertEquals(expectedAsn, decoder.getAsnNumber(mapWithAsn));
    }

    @Test
    public void testMalformedAsnNumberReturnsNull() {
        Map<String, Object> mapWithMalformedAsn = new HashMap<>();
        // ASN is a string instead of a long
        mapWithMalformedAsn.put(MaxmindAsnResponseDecoder.AS_NUM_KEYWORD, "500");
        assertNull(decoder.getAsnNumber(mapWithMalformedAsn));
    }

    @Test
    public void testValidAsnOrg() {
        Map<String, Object> mapWithOrg = new HashMap<>();
        String expectedOrg = "org name";
        mapWithOrg.put(MaxmindAsnResponseDecoder.AS_ORG_KEYWORD, expectedOrg);
        assertEquals(expectedOrg, decoder.getAutonomousSystemOrganization(mapWithOrg));
    }
}