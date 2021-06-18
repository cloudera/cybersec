package com.cloudera.parserchains.core;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.nio.charset.StandardCharsets;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The value of a field contained within a {@link Message}.
 */
public class FieldValue {
    static final int MAX_LENGTH = 65_536;
    private final String value;
    private final byte[] byteValue;

    public static FieldValue of(String fieldValue) {
        return new FieldValue(fieldValue);
    }

    public static FieldValue of(byte[] fieldValue) {
        return new FieldValue(fieldValue);
    }

    /**
     * Instead use {@link FieldValue#of(String)}.
     */
    private FieldValue(String value) {
        if(value == null || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Invalid field value.");
        }
        this.value = value;
        this.byteValue = null;
    }

    private FieldValue(byte[] byteValue) {
        if (byteValue == null) {
            throw new IllegalArgumentException("Null byteValue supplied to constructor");
        }
        this.value = null;
        this.byteValue = byteValue;
    }

    public String get() {
        return toString();
    }

    public byte[] getByteValue() {
        if (value != null) {
            return value.getBytes(UTF_8);
        }
        return byteValue;
    }

    @Override
    public String toString() {
        if (value != null) {
            return value;
        } else if (byteValue != null) {
            return new String(byteValue, StandardCharsets.UTF_8);
        } else {
            return "null";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldValue that = (FieldValue) o;
        return new EqualsBuilder()
                .append(value, that.value)
                .append(byteValue, that.byteValue)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(value)
                .append(byteValue)
                .toHashCode();
    }
}
