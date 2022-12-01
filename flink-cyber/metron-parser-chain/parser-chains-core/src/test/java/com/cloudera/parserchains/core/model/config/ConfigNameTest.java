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

package com.cloudera.parserchains.core.model.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigNameTest {

    @Test
    void valid() {
        ConfigName.of("Letters are allowed");
        ConfigName.of("Numbers like 0123456789 are allowed");
        ConfigName.of("Whitespace is allowed        ");
        ConfigName.of("Some punctuation is allowed , - . : ");
        ConfigName.of("A name of up to 40 characters is allowed");
        ConfigName.of("Grok Expression(s)");
    }

    @Test
    void tooLong() {
        String tooLong = "A name of over 40 characters is NOT allowed";
        assertThrows(IllegalArgumentException.class, () -> ConfigName.of(tooLong));
    }

    @Test
    void tooShort() {
        assertThrows(IllegalArgumentException.class, () -> ConfigName.of(""));
    }

    @Test
    void notNull() {
        assertThrows(IllegalArgumentException.class, () -> ConfigName.of(null));
    }

    @Test
    void invalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> ConfigName.of("<html></html>"));
    }

    @Test
    void get() {
        String name = "config_name";
        assertEquals(name, ConfigName.of(name).get());
    }
}
