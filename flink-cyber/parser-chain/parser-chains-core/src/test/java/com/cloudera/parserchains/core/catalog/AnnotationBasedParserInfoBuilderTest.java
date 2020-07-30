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
