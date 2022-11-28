/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

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
