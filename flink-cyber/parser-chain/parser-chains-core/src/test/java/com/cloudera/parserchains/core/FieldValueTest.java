package com.cloudera.parserchains.core;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FieldValueTest {
    private static final String value = "<14>1 2014-06-20T09:14:07+00:00 loggregator"
            + " d0602076-b14a-4c55-852a-981e7afeed38 DEA MSG-01"
            + " [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"]"
            + "[exampleSDID@32480 iut=\"4\" eventSource=\"Other Application\" eventID=\"2022\"] "
            + "Removing instance";

    @Test
    void valid() {
        FieldValue.of(value);
    }

    @Test
    void tooLong() {
        String tooLong = StringUtils.repeat("A", FieldValue.MAX_LENGTH+1);
        assertThrows(IllegalArgumentException.class, () -> FieldValue.of(tooLong));
    }

    @Test
    void notNull() {
        assertThrows(IllegalArgumentException.class, () -> FieldValue.of(null));
    }

    @Test
    void get() {
        assertEquals(value, FieldValue.of(value).get());
    }
}
