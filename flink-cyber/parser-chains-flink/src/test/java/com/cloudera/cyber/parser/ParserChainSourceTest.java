package com.cloudera.cyber.parser;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ParserChainSourceTest {
    private static final String CONTEXT = "context";

    @Test
    public void testValidParserChainSource() {
        String expectedChainKey = "my_parser_chain";
        String expectedSource = "my_event_source";
        ParserChainSource parserChainSource = new ParserChainSource(expectedChainKey, expectedSource);

        assertThatCode(() -> parserChainSource.validate(CONTEXT)).doesNotThrowAnyException();
    }

    @Test
    public void testNulls() {
        testNullValidationThrows(null, "validSource");
        testNullValidationThrows("", "validSource");
        testNullValidationThrows("validChainKey", null);
        testNullValidationThrows("validChainKey", "");

        testNullValidationThrows(null, null);
        testNullValidationThrows("", "");
    }

    private void testNullValidationThrows(String expectedChainKey, String expectedSource) {
        String expectedNullFieldName = (StringUtils.isEmpty(expectedChainKey) ? "chainKey" : "source");
        ParserChainSource parserChainSource = new ParserChainSource(expectedChainKey, expectedSource);
        assertThatThrownBy(() -> parserChainSource.validate(CONTEXT)).isInstanceOf(IllegalArgumentException.class).
                hasMessage(String.format(ParserChainSource.NULL_VALIDATION_ERROR, expectedNullFieldName, CONTEXT));
    }

}
