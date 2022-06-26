package com.cloudera.parserchains.core;

import com.cloudera.cyber.parser.MessageToParse;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The value of a field contained within a {@link Message}.
 */
public interface FieldValue {
    String get();
    byte[] toBytes();
    MessageToParse toMessageToParse();
}
