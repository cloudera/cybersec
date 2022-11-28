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

package com.cloudera.parserchains.core.model.define;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Objects;

/**
 * The name of a parser.
 */
public class ParserName implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonValue
    private final String name;

    /**
     * Create a new {@link ParserName}.
     * @param name The name of the parser.
     */
    @JsonCreator
    public static final ParserName of(String name) {
        return new ParserName(name);
    }

    /**
     * Returns the {@link ParserName} associated with a router.
     */
    public static ParserName router() {
        return ParserName.of("Router");
    }

    /**
     * Private constructor, use {@link ParserName#of(String)} instead.
     */
    private ParserName(String name) {
        this.name = Objects.requireNonNull(name, "A valid name is required.");
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParserName that = (ParserName) o;
        return new EqualsBuilder()
                .append(name, that.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ParserName{" +
                "name='" + name + '\'' +
                '}';
    }
}
