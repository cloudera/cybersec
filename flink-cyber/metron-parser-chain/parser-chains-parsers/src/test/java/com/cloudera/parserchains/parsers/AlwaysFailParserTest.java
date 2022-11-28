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

package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AlwaysFailParserTest {

    @Test
    void alwaysFail() {
        final String expectedMessage = "Unexpected value found.";
        Message input = Message.builder().build();
        Message output = new AlwaysFailParser()
                .withError(expectedMessage)
                .parse(input);

        assertTrue(output.getError().isPresent(), 
            "Expected the parser to always fail.");
        assertEquals(IllegalStateException.class, output.getError().get().getClass(), 
            "Expected the parser to capture an exception.");
        assertEquals(expectedMessage, output.getError().get().getMessage(),
            "Expected the parser to report the error message.");
    }

    @Test
    void failWithDefaultMessage() {
        AlwaysFailParser parser = new AlwaysFailParser();        
        Message input = Message.builder().build();
        Message output = new AlwaysFailParser()
                .parse(input);

        final String expectedMessage = parser.getError().getMessage();
        assertEquals(expectedMessage, output.getError().get().getMessage(),
                "Expected the parser to report the default error message.");
        assertTrue(output.getError().isPresent(),
                "Expected the parser to always fail.");
        assertEquals(IllegalStateException.class, output.getError().get().getClass(),
                "Expected the parser to capture an exception.");
    }

    @Test
    void configure() {
        String expected = "a custom error message";
        AlwaysFailParser parser = new AlwaysFailParser();
        parser.withError(expected);
        assertEquals(expected, parser.getError().getMessage());
    }
}
