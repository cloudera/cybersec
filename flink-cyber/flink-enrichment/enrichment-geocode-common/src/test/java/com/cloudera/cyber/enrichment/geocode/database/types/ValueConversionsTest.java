package com.cloudera.cyber.enrichment.geocode.database.types;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

    @Test
    public void testSafeLookupWithSpecifiedType() {
        String stringKey = "key for string";
        String stringValue = "the value";
        String longKey = "key for long";
        Map<String, Object> map = new HashMap<>();
        map.put(stringKey, stringValue);
        map.put(longKey, 5L);

        // test key found with specified type - return value
        assertEquals(stringValue, ValueConversions.safeLookup(map, stringKey, String.class));
        // test key found but value type does not match
        assertNull(ValueConversions.safeLookup(map, longKey, String.class));
    }

    @Test
    public void testSafeLookupNullInputs() {
        // if any of the inputs are null, return null
        assertNull(ValueConversions.safeLookup(null, "key", String.class));
        assertNull(ValueConversions.safeLookup(Collections.emptyMap(), "key", null));
        assertNull(ValueConversions.safeLookup(Collections.emptyMap(), null, String.class));
    }

    @Test
    public void testSafeLookupKeyNotFound() {
        assertNull(ValueConversions.safeLookup(Collections.emptyMap(), "key", String.class));
    }

    @Test
    public void testAsnConversion() {
        assertEquals(123L, ValueConversions.extractIpinfoAsnNumber("AS123"));
        assertNull(ValueConversions.extractIpinfoAsnNumber(null));
        assertNull(ValueConversions.extractIpinfoAsnNumber("AS"));
        assertNull(ValueConversions.extractIpinfoAsnNumber("123"));
    }
}
