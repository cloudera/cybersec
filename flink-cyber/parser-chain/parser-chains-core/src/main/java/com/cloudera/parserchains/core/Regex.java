package com.cloudera.parserchains.core;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A regular expression.
 */
public final class Regex {
    private final String regex;
    private final Pattern pattern;

    public static final Regex of(String regex) {
        return new Regex(regex);
    }

    /**
     * Instead use {@link Regex#of(String)}.
     */
    private Regex(String regex) {
        this.regex = Objects.requireNonNull(regex, "A valid regular expression is required.");
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Tells whether a field value matches this regular expression.
     * @param fieldValue The value to match.
     * @return True if the field value matches the regular expression. Otherwise, false.
     */
    public boolean matches(FieldValue fieldValue) {
        return matches(fieldValue.toString());
    }

    /**
     * Tells whether a field name matches this regular expression.
     * @param fieldName The value to match.
     * @return True if the field value matches the regular expression. Otherwise, false.
     */
    public boolean matches(FieldName fieldName) {
        return matches(fieldName.toString());
    }

    /**
     * Tells whether a string matches this regular expression.
     * @param value The value to match
     * @return True if the value matches the regular expression. Otherwise, false.
     */
    public boolean matches(String value) {
        if(value == null) {
            return false;
        }
        return pattern.matcher(value).matches();
    }

    @Override
    public String toString() {
        return regex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Regex that = (Regex) o;
        return new EqualsBuilder()
                .append(regex, that.regex)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(regex)
                .toHashCode();
    }
}
