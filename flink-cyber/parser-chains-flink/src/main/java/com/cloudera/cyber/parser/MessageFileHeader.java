package com.cloudera.cyber.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
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
    public static final String NULL_HEADER_PREFIX_CONFIG = "MessageFileHeader headerPrefixes has null entry.";
    public static final String EMPTY_HEADER_PREFIX_CONFIG = "MessageFileHeader headerPrefixes has empty string entry.";
    public static final String NULL_HEADER_REQUIRED_HEADERS_CONFIG = "MessageFileHeader has null or empty requiredHeaders when header is enabled.";
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
        boolean hasLineCount = headerLineCount != null && headerLineCount > 0;
        boolean hasPrefixes = headerPrefixes != null && !headerPrefixes.isEmpty();
        
        // Must specify at least one method
        Preconditions.checkArgument(hasLineCount || hasPrefixes, HEADER_LINE_COUNT_OR_PREFIXES_REQUIRED);
        
        // Cannot use both methods
        Preconditions.checkArgument(!(hasLineCount && hasPrefixes), HEADER_LINE_COUNT_AND_PREFIXES_CONFLICT);

        if (hasPrefixes) {
            for (String prefix : headerPrefixes) {
                Preconditions.checkNotNull(prefix, NULL_HEADER_PREFIX_CONFIG);
                Preconditions.checkArgument(StringUtils.isNotEmpty(prefix), EMPTY_HEADER_PREFIX_CONFIG);
            }
        }

        if (requiredHeaders != null) {
            Preconditions.checkArgument(!requiredHeaders.isEmpty(), NULL_HEADER_REQUIRED_HEADERS_CONFIG);
            for (String requiredHeader : requiredHeaders) {
                Preconditions.checkArgument(StringUtils.isNotEmpty(requiredHeader), EMPTY_HEADER_REQUIRED_HEADERS_CONFIG);
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
}