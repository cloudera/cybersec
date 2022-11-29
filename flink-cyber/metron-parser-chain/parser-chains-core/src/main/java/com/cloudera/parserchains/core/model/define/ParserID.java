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

import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Objects;

/**
 * Uniquely identifies a parser.
 */
public class ParserID implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonValue
    private final String id;
    
    /**
     * Creates a new {@link ParserID} from a Parser class.
     * @param clazz The Parser class.
     * @return
     */
    public static ParserID of(Class<?> clazz) {
        return new ParserID(clazz.getCanonicalName());
    }

    /**
     * Returns the {@link ParserID} used to identify a router.
     */
    public static ParserID router() {
        return new ParserID("Router");
    }

    /**
     * Private constructor.  See {@link ParserID#of(Class)}.
     */
    private ParserID(String id) {
        this.id = Objects.requireNonNull(id, "A valid ID is required.");
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParserID parserID = (ParserID) o;
        return new EqualsBuilder()
                .append(id, parserID.id)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ParserID{" +
                "id='" + id + '\'' +
                '}';
    }
}
