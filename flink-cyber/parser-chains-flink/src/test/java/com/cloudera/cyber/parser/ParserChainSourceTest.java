package com.cloudera.cyber.parser;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ParserChainSourceTest {
    private static final String CONTEXT = "context";

    @Test
    public void testValidParserChainSourceWithoutHeader() {
        String expectedChainKey = "my_parser_chain";
        String expectedSource = "my_event_source";
        ParserChainSource parserChainSource = new ParserChainSource(expectedChainKey, expectedSource, null);

        assertThatCode(() -> parserChainSource.validate(CONTEXT)).doesNotThrowAnyException();
        assertThat(parserChainSource.hasHeader()).isFalse();
        assertThat(parserChainSource.getHeaderLineCount()).isEqualTo(0);
        assertThat(parserChainSource.getHeaderPrefixes()).isEmpty();
        assertThat(parserChainSource.usesHeaderLineCount()).isFalse();
        assertThat(parserChainSource.usesHeaderPrefixes()).isFalse();
        assertThat(parserChainSource.getRequiredHeaders()).isNull();
    }

    @Test
    public void testValidParserChainSourceWithHeader() {
        // using line count - with and without required headers
        testParserChainSourceWithHeader(1, null, null);
        testParserChainSourceWithHeader(1, null, Collections.singletonList("required header"));

        // using prefix - with and without required headers
        testParserChainSourceWithHeader(null, Collections.singletonList("###"), null);
        testParserChainSourceWithHeader(null, Collections.singletonList("###"), Collections.singletonList("required header"));
    }

    private static void testParserChainSourceWithHeader(Integer headerLineCount, List<String> headerPrefixes, List<String> requiredHeaders) {
        String expectedChainKey = "my_parser_chain";
        String expectedSource = "my_event_source";

        MessageFileHeader header = new MessageFileHeader(headerLineCount, headerPrefixes, requiredHeaders);
        ParserChainSource parserChainSource = new ParserChainSource(expectedChainKey, expectedSource, header);

        assertThatCode(() -> parserChainSource.validate(CONTEXT)).doesNotThrowAnyException();

        assertThat(parserChainSource.hasHeader()).isTrue();

        if (headerLineCount != null) {
            assertThat(parserChainSource.usesHeaderLineCount()).isTrue();
            assertThat(parserChainSource.getHeaderLineCount()).isEqualTo(headerLineCount);
            assertThat(parserChainSource.usesHeaderPrefixes()).isFalse();
            assertThat(parserChainSource.getHeaderPrefixes()).isNull();
        } else {
            assertThat(parserChainSource.usesHeaderLineCount()).isFalse();
            assertThat(parserChainSource.getHeaderLineCount()).isEqualTo(0);
            assertThat(parserChainSource.usesHeaderPrefixes()).isTrue();
            assertThat(parserChainSource.getHeaderPrefixes()).containsExactlyInAnyOrderElementsOf(headerPrefixes);
        }

        if (requiredHeaders == null) {
            assertThat(parserChainSource.getRequiredHeaders()).isNull();
        } else {
            assertThat(parserChainSource.getRequiredHeaders()).containsExactlyInAnyOrderElementsOf(requiredHeaders);
        }
    }

    @Test
    public void testNulls() {
        testNullValidationThrows(null, "validSource");
        testNullValidationThrows("validChainKey", null);
        testNullValidationThrows(null, null);
    }

    @Test
    public void testEmptyValidation() {
        testEmptyValidationThrows("", "validSource");
        testEmptyValidationThrows("validChainKey", "");
        testEmptyValidationThrows("", "");
    }

    private void testNullValidationThrows(String expectedChainKey, String expectedSource) {
        String nullFieldName = (expectedChainKey == null ? "chainKey" : "source");

        assertThatThrownBy(() -> new ParserChainSource(expectedChainKey, expectedSource, null)).isInstanceOf(NullPointerException.class).
                hasMessage(String.format("%s is marked non-null but is null", nullFieldName));
    }

    public void testEmptyValidationThrows(String expectedChainKey, String expectedSource) {
        String expectedNullFieldName = (StringUtils.isEmpty(expectedChainKey) ? "chainKey" : "source");
        ParserChainSource parserChainSource = new ParserChainSource(expectedChainKey, expectedSource, null);
        assertThatThrownBy(() -> parserChainSource.validate(CONTEXT)).isInstanceOf(IllegalArgumentException.class).
                hasMessage(String.format(ParserChainSource.NULL_OR_EMPTY_VALIDATION_ERROR, expectedNullFieldName, CONTEXT));
    }

}
