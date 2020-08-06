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
