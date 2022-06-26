package com.cloudera.parserchains.core;

import com.cloudera.cyber.parser.MessageToParse;
import lombok.EqualsAndHashCode;

import java.nio.charset.StandardCharsets;

@EqualsAndHashCode
public class MessageToParseFieldValue implements FieldValue {
    private final MessageToParse value;

    public static FieldValue of(MessageToParse fieldValue) {
        return new MessageToParseFieldValue(fieldValue);
    }

    /**
     * Instead use {@link MessageToParseFieldValue#of(MessageToParse)}.
     */
    private MessageToParseFieldValue(MessageToParse value) {
        this.value = value;
    }

    public String get() {
        return toString();
    }

    public byte[] toBytes() {
        return value.getOriginalBytes();
    }

    public String toString() {
        return value != null ? new String(value.getOriginalBytes(), StandardCharsets.UTF_8) : "null";
    }

    @Override
    public MessageToParse toMessageToParse() {
        return value;
    }


}
