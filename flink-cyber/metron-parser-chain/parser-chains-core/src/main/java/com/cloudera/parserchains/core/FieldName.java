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

/**
 * The name of a field contained within a {@link Message}.
 */
public class FieldName {
    private static final Regex validFieldName = Regex.of("^[a-zA-Z_0-9 ,-.:;#@\\|\\[\\]]{1,120}$");
    private final String fieldName;

    public static final FieldName of(String fieldName) {
        return new FieldName(fieldName);
    }

    /**
     * Use {@link FieldName#of(String)}.
     */
    private FieldName(String fieldName) {
        if (!validFieldName.matches(fieldName)) {
            throw new IllegalArgumentException(String.format("Invalid field name: '%s'", fieldName));
        }
        this.fieldName = fieldName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FieldName that = (FieldName) o;
        return new EqualsBuilder()
              .append(fieldName, that.fieldName)
              .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
              .append(fieldName)
              .toHashCode();
    }

    public String get() {
        return fieldName;
    }

    @Override
    public String toString() {
        return fieldName;
    }
}
