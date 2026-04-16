package com.cloudera.cyber.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.util.Preconditions;

import java.io.Serializable;
import java.util.List;

/**
 * Header configuration for message file parsing.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageFileHeader implements Serializable {
    public static final String EMPTY_HEADER_PREFIX_CONFIG = "MessageFileHeader has at least one headerPrefix that is null or empty when using prefixes to remove headers.";
    public static final String EMPTY_HEADER_REQUIRED_HEADERS_CONFIG = "MessageFileHeader has at least one requiredHeader that is null or empty.";
    public static final String EMPTY_HEADER_REQUIRED_HEADERS_CONFIG = "MessageFileHeader has at least one null or empty requiredHeader in requiredHeaders when header is enabled.";
    public static final String HEADER_LINE_COUNT_AND_PREFIXES_CONFLICT = "Only one of headerLineCount or headerPrefixes can be specified, not both.";
    public static final String HEADER_LINE_COUNT_OR_PREFIXES_REQUIRED = "Either headerLineCount or headerPrefixes must be specified.";

    /**
     * Number of header lines to skip (mutually exclusive with headerPrefixes).
     */
    private Integer headerLineCount;

    /**
     * Prefix strings to identify header lines (mutually exclusive with headerLineCount).
     */
    private List<String> headerPrefixes;

    /**
     * Required header names that must exist in file headers.
     */
    private List<String> requiredHeaders;

    /**
     * Validates this header configuration.
     */
    public void validate() {
        boolean usesLineCount = usesHeaderLineCount();
        boolean usesPrefixes = usesHeaderPrefixes();
        // Must specify at least one method
        Preconditions.checkArgument(usesLineCount || usesPrefixes, HEADER_LINE_COUNT_OR_PREFIXES_REQUIRED);
        
        // Cannot use both methods
        Preconditions.checkArgument(!(usesLineCount && usesPrefixes), HEADER_LINE_COUNT_AND_PREFIXES_CONFLICT);

        if (usesPrefixes) {
            for(String headerPrefix : headerPrefixes) {
                Preconditions.checkArgument(headerPrefix != null && !headerPrefix.isEmpty(), EMPTY_HEADER_PREFIX_CONFIG);
            }
        }

        if (requiredHeaders != null) {
            for(String requiredHeader : requiredHeaders) {
                Preconditions.checkArgument(requiredHeader != null && !requiredHeader.isEmpty(), EMPTY_HEADER_REQUIRED_HEADERS_CONFIG);
            }
        }
    }

    /**
     * Returns true if using line count method for header detection.
     */
    public boolean usesHeaderLineCount() {
        return headerLineCount != null && headerLineCount > 0;
    }

    /**
     * Returns true if using prefix method for header detection.
     */
    public boolean usesHeaderPrefixes() {
        return headerPrefixes != null && !headerPrefixes.isEmpty();
    }

    public int getHeaderLineCount() {
        return headerLineCount != null ? headerLineCount : 0;
    }
}