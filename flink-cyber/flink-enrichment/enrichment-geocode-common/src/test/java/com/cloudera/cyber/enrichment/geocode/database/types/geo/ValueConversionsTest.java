package com.cloudera.cyber.enrichment.geocode.database.types.geo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ValueConversionsTest {

    @Test
    public void testConvertEmptyToNull() {
        String nonNull = "a value";
        assertEquals(nonNull, ValueConversions.convertEmptyToNull(nonNull));
        assertNull(ValueConversions.convertEmptyToNull(""));
        assertNull(ValueConversions.convertEmptyToNull(null));
    }

    @Test
    public void testConvertNullToEmpty() {
        String nonNullString = "a string";

        assertEquals(nonNullString, ValueConversions.convertNullToEmptyString(nonNullString));
        assertEquals("", ValueConversions.convertNullToEmptyString(null));
        assertEquals("5", ValueConversions.convertNullToEmptyString(5L));
    }
}
