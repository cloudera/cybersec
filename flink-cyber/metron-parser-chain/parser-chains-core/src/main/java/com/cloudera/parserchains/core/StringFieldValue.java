package com.cloudera.parserchains.core;

import com.cloudera.cyber.parser.MessageToParse;
import lombok.EqualsAndHashCode;
import lombok.ToString;


import static java.nio.charset.StandardCharsets.UTF_8;

@EqualsAndHashCode
public class StringFieldValue implements FieldValue {
    static final int MAX_LENGTH = 65_536;
    private final String value;

    public static FieldValue of(String fieldValue) {
        return new StringFieldValue(fieldValue);
    }

    /**
     * Instead use {@link StringFieldValue#of(String)}.
     */
    private StringFieldValue(String value) {
        if(value == null || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Invalid field value.");
        }
        this.value = value;
    }

    public String get() {
        return value != null ? value : "null";
    }

    public byte[] toBytes() {
        return value != null ? value.getBytes(UTF_8) : null;
    }

    public String toString() {
        return (value != null) ? value : "null";
    }
    @Override
    public MessageToParse toMessageToParse() {
        return MessageToParse.builder().originalBytes(toBytes()).offset(0).partition(0).topic("none").build();
    }

}
