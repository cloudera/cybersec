package com.cloudera.parserchains.core;

import com.cloudera.parserchains.core.model.define.ParserName;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LinkNameTest {
    private static final String name = "parser1";
    static ParserName parserName = ParserName.of("Some Test Parser");

  @Test
    void valid() {
        LinkName.of("Link names can contain letters and whitespace", parserName);
        LinkName.of("Link names can contain numbers 0123456789", parserName);
        LinkName.of("Link names can contain some punctuation  , - . : @", parserName);
        String maxLengthName = StringUtils.repeat("A", 120);
        LinkName.of(maxLengthName, parserName);
    }

    @Test
    void uuidIsValid() {
        LinkName.of(UUID.randomUUID().toString(), parserName);
        LinkName.of(UUID.randomUUID().toString(), parserName);
        LinkName.of(UUID.randomUUID().toString(), parserName);
        LinkName.of(UUID.randomUUID().toString(), parserName);
    }

    @Test
    void tooLong() {
        String tooLong = StringUtils.repeat("A", 121);
        assertThrows(IllegalArgumentException.class, () -> LinkName.of(tooLong,
            parserName));
    }

    @Test
    void tooShort() {
        assertThrows(IllegalArgumentException.class, () -> LinkName.of("", parserName));
    }

    @Test
    void notNull() {
        assertThrows(IllegalArgumentException.class, () -> LinkName.of(null, parserName));
    }

    @Test
    void invalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> LinkName.of("<html></html>",
            parserName));
    }

    @Test
    void get() {
        assertEquals(name, LinkName.of(name, parserName).getLinkName());
    }
}
