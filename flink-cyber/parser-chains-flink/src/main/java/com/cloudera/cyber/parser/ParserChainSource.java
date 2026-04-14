package com.cloudera.cyber.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.util.Preconditions;

import java.io.Serializable;

/**
 * Represents a parser chain source configuration.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ParserChainSource implements Serializable {
    public static final String NULL_VALIDATION_ERROR = "%s for %s is null";

    private String chainKey;
    private String source;
    private MessageFileHeader messageFileHeader;

    public void validate(String context) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(chainKey), NULL_VALIDATION_ERROR, "chainKey", context);
        Preconditions.checkArgument(StringUtils.isNotEmpty(source), NULL_VALIDATION_ERROR, "source", context);
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
}
