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

package com.cloudera.parserchains.core.catalog;

import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotationBasedParserInfoBuilderTest {

    /**
     * A parser used for testing that is completely valid.
     */
    @MessageParser(
            name = "Valid Parser",
            description = "This is a valid parser.")
    public class ValidParser implements Parser {
        @Override
        public Message parse(Message message) {
            // do nothing
            return null;
        }
    }

    @Test
    void build() {
        Optional<ParserInfo> result = new AnnotationBasedParserInfoBuilder()
                .build(ValidParser.class);
        assertTrue(result.isPresent());
        assertEquals("Valid Parser", result.get().getName());
        assertEquals("This is a valid parser.", result.get().getDescription());
        assertEquals(ValidParser.class, result.get().getParserClass());
    }

    /**
     * A parser used for testing that is missing the required {@link MessageParser} annotation.
     */
    public class MissingAnnotationParser implements Parser {
        @Override
        public Message parse(Message message) {
            // do nothing
            return null;
        }
    }

    @Test
    void classIsMissingAnnotation() {
        Optional<ParserInfo> result = new AnnotationBasedParserInfoBuilder()
                .build(MissingAnnotationParser.class);
        assertFalse(result.isPresent());
    }

    @Test
    void classIsNotAParser() {
        Optional<ParserInfo> result = new AnnotationBasedParserInfoBuilder()
                .build(Object.class);
        assertFalse(result.isPresent());
    }
}
