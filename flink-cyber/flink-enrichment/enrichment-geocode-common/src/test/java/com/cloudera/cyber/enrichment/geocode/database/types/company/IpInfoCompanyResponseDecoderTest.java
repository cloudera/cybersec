package com.cloudera.cyber.enrichment.geocode.database.types.company;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class IpInfoCompanyResponseDecoderTest {

    private final IpInfoCompanyResponseDecoder decoder = new IpInfoCompanyResponseDecoder();

    @Test
    void testGetCompanyWithNullMap() {
        assertNull(decoder.getCompany(null));
    }

    @Test
    void testGetCompanyWithEmptyMap() {
        assertNull(decoder.getCompany(Collections.emptyMap()));
    }

    @Test
    void testGetCompanyReturnsNameValue() {
        Map<String, Object> data = new HashMap<>();
        String expectedCompany = "Example Corp";
        data.put(IpInfoCompanyResponseDecoder.COMPANY_KEY, expectedCompany);
        
        assertEquals(expectedCompany, decoder.getCompany(data));
    }

    @Test
    void testGetAsnNumberWithNullMap() {
        assertNull(decoder.getAsnNumber(null));
    }

    @Test
    void testGetAsnNumberWithEmptyMap() {
        assertNull(decoder.getAsnNumber(Collections.emptyMap()));
    }

    @Test
    void testGetAsnNumberParsesValidAsn() {
        Map<String, Object> data = new HashMap<>();
        data.put(IpInfoCompanyResponseDecoder.ASN_KEY, "AS12345");
        
        assertEquals(12345L, decoder.getAsnNumber(data));
    }

    @Test
    void testGetAsnNumberWithInvalidFormat() {
        Map<String, Object> data = new HashMap<>();
        data.put(IpInfoCompanyResponseDecoder.ASN_KEY, "12345");
        
        assertNull(decoder.getAsnNumber(data));
    }

    @Test
    void testGetAsnNumberWithNonNumericAsn() {
        Map<String, Object> data = new HashMap<>();
        data.put(IpInfoCompanyResponseDecoder.ASN_KEY, "ASABC");
        
        assertNull(decoder.getAsnNumber(data));
    }

    @Test
    void testGetAutonomousSystemOrganizationWithNullMap() {
        assertNull(decoder.getAutonomousSystemOrganization(null));
    }

    @Test
    void testGetAutonomousSystemOrganizationWithEmptyMap() {
        assertNull(decoder.getAutonomousSystemOrganization(Collections.emptyMap()));
    }

    @Test
    void testGetAutonomousSystemOrganizationReturnsValue() {
        Map<String, Object> data = new HashMap<>();
        String expectedOrg = "Example ISP";
        data.put(IpInfoCompanyResponseDecoder.AS_NAME_KEY, expectedOrg);
        
        assertEquals(expectedOrg, decoder.getAutonomousSystemOrganization(data));
    }
}
