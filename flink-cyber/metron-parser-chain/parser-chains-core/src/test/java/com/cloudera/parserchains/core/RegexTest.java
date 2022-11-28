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

import org.junit.jupiter.api.Test;

import java.util.regex.PatternSyntaxException;

import static org.junit.jupiter.api.Assertions.*;

public class RegexTest {
    
    @Test
    void matchString() {
        Regex regex = Regex.of("^[A-Z]{1,2}$");
        assertFalse(regex.matches(""));
        assertTrue(regex.matches("B"));
        assertTrue(regex.matches("BB"));
        assertFalse(regex.matches("BBB"));
    }

    @Test
    void matchFieldName() {
        Regex regex = Regex.of("^[A-Z]{0,2}$");
        assertTrue(regex.matches(FieldName.of("B")));
        assertTrue(regex.matches(FieldName.of("BB")));
        assertFalse(regex.matches(FieldName.of("BBB")));
    }

    @Test
    void matchFieldValue() {
        Regex regex = Regex.of("^[A-Z]{0,2}$");
        assertTrue(regex.matches(StringFieldValue.of("B")));
        assertTrue(regex.matches(StringFieldValue.of("BB")));
        assertFalse(regex.matches(StringFieldValue.of("BBB")));
    }
    
    @Test
    void invalidRegex() {
        assertThrows(PatternSyntaxException.class, () -> Regex.of("[[["));
    }
    
    @Test
    void noNulls() {
        assertThrows(NullPointerException.class, () -> Regex.of(null));
    }
}
