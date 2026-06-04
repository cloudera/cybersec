package com.cloudera.cyber.enrichment.geocode.database.types.asn;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Common tests for AsnDatabaseResponseDecoder subclasses.
 * This test is disabled because it should only be run as part of a subclass.
 */
@Disabled
public class AsnDatabaseResponseBaseTest {
    protected final AsnDatabaseResponseDecoder decoder;

    protected AsnDatabaseResponseBaseTest(AsnDatabaseResponseDecoder decoder) {
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
        assertNull(decoder.getAsnNumber(response));
        assertNull(decoder.getAutonomousSystemOrganization(response));
    }

}
