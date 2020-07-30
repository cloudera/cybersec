package com.cloudera.parserchains.core.model.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigDescriptionTest {

    @Test
    void valid() {
        ConfigDescription.of("A description contains letters, numbers, whitespace, and punctuation.");
        ConfigDescription.of("A description can contain between 1 and 80 characters.");
        ConfigDescription.of("A description can contain 'single' or \"double\" quotes.");
        ConfigDescription.of("Set the zone offset. For example \"+02:00\".");
    }

    @Test
    void tooShort() {
        assertThrows(IllegalArgumentException.class, () -> ConfigDescription.of(null));
        assertThrows(IllegalArgumentException.class, () -> ConfigDescription.of(""));
    }

    @Test
    void tooLong() {
        String tooLong = "This description exceeds the max allowable number of characters and is far too long.";
        assertThrows(IllegalArgumentException.class, () -> ConfigDescription.of(tooLong));
    }

    @Test
    void invalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> ConfigDescription.of("<html></html>"));
    }

    @Test
    void notNull() {
        assertThrows(IllegalArgumentException.class, () -> ConfigDescription.of(null));
    }
}
