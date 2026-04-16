package com.cloudera.cyber.parser;

import lombok.*;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.util.Preconditions;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Represents a parser chain source configuration.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParserChainSource implements Serializable {
    public static final String NULL_OR_EMPTY_VALIDATION_ERROR = "%s for %s is null or empty";

    @NonNull
    private String chainKey;
    @NonNull
    private String source;
    private MessageFileHeader messageFileHeader;

    public void validate(String context) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(chainKey), NULL_OR_EMPTY_VALIDATION_ERROR, "chainKey", context);
        Preconditions.checkArgument(StringUtils.isNotEmpty(source), NULL_OR_EMPTY_VALIDATION_ERROR, "source", context);
        if (messageFileHeader != null) {
            messageFileHeader.validate();
        }
    }

    /**
     * Returns true if header processing is configured.
     */
    public boolean hasHeader() {
        return messageFileHeader != null;
    }

    public List<String> getRequiredHeaders() {
        return messageFileHeader != null ? messageFileHeader.getRequiredHeaders() : null;
    }

    /**
     * Returns true if using line count method for header detection.
     */
    public boolean usesHeaderLineCount() {
        return messageFileHeader != null && messageFileHeader.usesHeaderLineCount();
    }

    /**
     * Returns true if using prefix method for header detection.
     */
    public boolean usesHeaderPrefixes() {
        return messageFileHeader != null && messageFileHeader.usesHeaderPrefixes();
    }

    /**
     * Returns the header line count.
     */
    public int getHeaderLineCount() {
        return messageFileHeader != null  ? messageFileHeader.getHeaderLineCount() : 0;
    }

    /**
     * Returns the header prefixes.
     */
    public List<String> getHeaderPrefixes() {
        return messageFileHeader != null ? messageFileHeader.getHeaderPrefixes() : Collections.emptyList();
    }
}
