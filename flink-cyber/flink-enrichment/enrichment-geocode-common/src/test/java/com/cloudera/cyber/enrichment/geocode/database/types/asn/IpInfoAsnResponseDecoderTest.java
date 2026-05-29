package com.cloudera.cyber.enrichment.geocode.database.types.asn;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IpInfoAsnResponseDecoderTest extends AsnDatabaseResponseBaseTest {

    public IpInfoAsnResponseDecoderTest() {
        super(new IpInfoAsnResponseDecoder());
    }

    @Test
    public void testGetAsnNumber() {
        assertNull(decoder.getAsnNumber(Collections.emptyMap()));

        Map<String, Object> mapWithAsn = new HashMap<>();
        mapWithAsn.put(IpInfoAsnResponseDecoder.AS_NUM_KEYWORD, "AS500");
        assertEquals(500L, decoder.getAsnNumber(mapWithAsn));

        Map<String, Object> mapWithMalformedAsn = new HashMap<>();
        mapWithAsn.put(IpInfoAsnResponseDecoder.AS_NUM_KEYWORD, "500");
        assertNull(decoder.getAsnNumber(mapWithMalformedAsn));
    }

    @Test
    public void testGetAsnOrg() {
        assertNull(decoder.getAutonomousSystemOrganization(Collections.emptyMap()));

        Map<String, Object> mapWithOrg = new HashMap<>();
        String expectedOrg = "org name";
        mapWithOrg.put(IpInfoAsnResponseDecoder.AS_ORG_KEYWORD, expectedOrg);
        assertEquals(expectedOrg, decoder.getAutonomousSystemOrganization(mapWithOrg));
    }
}
