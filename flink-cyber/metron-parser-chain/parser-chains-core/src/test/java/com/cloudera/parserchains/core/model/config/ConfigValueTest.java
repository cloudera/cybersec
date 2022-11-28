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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigValueTest {

    @Test
    void valid() {
        ConfigValue.of("Letters are allowed");
        ConfigValue.of("Numbers like 0123456789 are allowed");
        ConfigValue.of("Whitespace is allowed        ");
        ConfigValue.of("Some punctuation is allowed , - . : ) (");
        ConfigValue.of("A name of up to 40 characters is allowed");
        ConfigValue.of("2 + 2");
        ConfigValue.of("%{UUID}");
        ConfigValue.of("+02:00");
    }

    @Test
    void tooLong() {
        String tooLong = StringUtils.repeat("a", 201);
        assertThrows(IllegalArgumentException.class, () -> ConfigValue.of(tooLong));
    }

    @Test
    void tooShort() {
        assertThrows(IllegalArgumentException.class, () -> ConfigValue.of(""));
    }

    @Test
    void notNull() {
        assertThrows(IllegalArgumentException.class, () -> ConfigValue.of(null));
    }

    @Test
    void get() {
        String name = "config_name";
        assertEquals(name, ConfigValue.of(name).get());
    }
}
