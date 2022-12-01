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
