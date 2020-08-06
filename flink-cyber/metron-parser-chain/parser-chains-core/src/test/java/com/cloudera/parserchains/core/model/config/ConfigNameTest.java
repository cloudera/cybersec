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
