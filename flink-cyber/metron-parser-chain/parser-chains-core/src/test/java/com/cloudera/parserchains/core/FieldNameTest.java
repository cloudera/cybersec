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

public class FieldNameTest {
    private static final String name = "syslog.structuredData.exampleSDID@32480.iut";

    @Test
    void valid() {
        FieldName.of(name);
        FieldName.of("Field names can contain letters and whitespace");
        FieldName.of("Field names can contain numbers 0123456789");
        FieldName.of("Field names can contain some punctuation  , - . : @ #");
        String maxLengthName = StringUtils.repeat("A", 120);
        FieldName.of(maxLengthName);
    }

    @Test
    void tooLong() {
        String tooLong = StringUtils.repeat("A", 121);
        assertThrows(IllegalArgumentException.class, () -> FieldName.of(tooLong));
    }

    @Test
    void tooShort() {
        assertThrows(IllegalArgumentException.class, () -> FieldName.of(""));
    }

    @Test
    void notNull() {
        assertThrows(IllegalArgumentException.class, () -> FieldName.of(null));
    }

    @Test
    void invalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> FieldName.of("<html></html>"));
    }

    @Test
    void get() {
        String name = "field_name";
        assertEquals(name, FieldName.of(name).get());
    }
}
