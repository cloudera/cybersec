package com.cloudera.cyber.parser;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class MessageFileHeaderTest {

    @Test
    public void testSuccessfulCreationAndQuery() {
        // line count method
        testSuccessfulMessageFileHeader(2, null, null);
        testSuccessfulMessageFileHeader(2, null, Collections.singletonList("required_header"));

        // prefix method
        testSuccessfulMessageFileHeader(null, Collections.singletonList("prefix"), null);
        testSuccessfulMessageFileHeader(null, Collections.singletonList("prefix"), Collections.singletonList("required_header"));
        testSuccessfulMessageFileHeader(0, Collections.singletonList("prefix"), null);
    }

    private void testSuccessfulMessageFileHeader(Integer headerLineCount, List<String> headerPrefixes, List<String> requiredHeaders) {
        MessageFileHeader messageFileHeader = new MessageFileHeader(headerLineCount, headerPrefixes, requiredHeaders);

        assertThatCode(messageFileHeader::validate).doesNotThrowAnyException();
        assertThat(messageFileHeader.usesHeaderLineCount()).isEqualTo(headerLineCount != null && headerLineCount > 0);
        assertThat(messageFileHeader.usesHeaderPrefixes()).isEqualTo(headerLineCount == null || headerLineCount == 0);
        assertThat(messageFileHeader.getHeaderLineCount()).isEqualTo(headerLineCount != null ? headerLineCount : 0);
        if (requiredHeaders != null) {
            assertThat(messageFileHeader.getRequiredHeaders()).
                    containsExactlyInAnyOrderElementsOf(requiredHeaders);
        } else {
            assertThat(messageFileHeader.getRequiredHeaders()).isNull();
        }
    }

    @Test
    public void testInvalidMessageHeader() {
        // neither method specified
        testFailure(null, null, null, MessageFileHeader.HEADER_LINE_COUNT_OR_PREFIXES_REQUIRED);
        // both methods specified
        testFailure(1, Collections.singletonList("prefix and number"), null, MessageFileHeader.HEADER_LINE_COUNT_AND_PREFIXES_CONFLICT);
        // empty and null string in prefix
        testFailure(null, Collections.singletonList(""), null, MessageFileHeader.EMPTY_HEADER_PREFIX_CONFIG);
        testFailure(null, Collections.singletonList(null), null, MessageFileHeader.EMPTY_HEADER_PREFIX_CONFIG);
        // empty and null string in required header
        testFailure(null, Collections.singletonList("valid prefix"), Collections.singletonList(""), MessageFileHeader.EMPTY_HEADER_REQUIRED_HEADERS_CONFIG);
        testFailure(null, Collections.singletonList("valid prefix"), Collections.singletonList(null), MessageFileHeader.EMPTY_HEADER_REQUIRED_HEADERS_CONFIG);
    }

    private void testFailure(Integer headerLineCount, List<String> headerPrefixes, List<String> requiredHeaders,
                             String expectedExceptionMessage) {
        MessageFileHeader messageFileHeader = new MessageFileHeader(headerLineCount, headerPrefixes, requiredHeaders);
        assertThatThrownBy(messageFileHeader::validate).isInstanceOf(IllegalArgumentException.class).hasMessage(expectedExceptionMessage);
    }
}
