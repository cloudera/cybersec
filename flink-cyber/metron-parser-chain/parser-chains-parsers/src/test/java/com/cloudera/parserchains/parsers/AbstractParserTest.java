package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Message;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AbstractParserTest {

    private static class TestParser extends AbstractTextInputParser {

        @Override
        public Message parse(Message message) {
            return null;
        }
    }

    @Test
    void getTestBytes() {
        TestParser parser = new TestParser();
        // test a string
        String testString = "this is a test";
        assertArrayEquals(testString.getBytes(StandardCharsets.UTF_8), parser.getTestBytes(testString));

        // test null
        assertNull(parser.getTestBytes(null));
    }
}
