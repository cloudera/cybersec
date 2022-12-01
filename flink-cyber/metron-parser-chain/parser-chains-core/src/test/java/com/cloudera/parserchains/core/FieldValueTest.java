/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

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
        StringFieldValue.of(value);
    }

    @Test
    void tooLong() {
        String tooLong = StringUtils.repeat("A", StringFieldValue.MAX_LENGTH+1);
        assertThrows(IllegalArgumentException.class, () -> StringFieldValue.of(tooLong));
    }

    @Test
    void notNullString() {
        String nullString = null;
        assertThrows(IllegalArgumentException.class, () -> StringFieldValue.of(nullString));
    }

    @Test
    void get() {
        assertEquals(value, StringFieldValue.of(value).get());
    }
}
