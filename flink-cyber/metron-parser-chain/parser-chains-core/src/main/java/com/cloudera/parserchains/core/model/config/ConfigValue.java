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

package com.cloudera.parserchains.core.model.config;

import com.cloudera.parserchains.core.Regex;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

import static com.cloudera.parserchains.core.Validator.mustMatch;

/**
 * The value associated with a {@link ConfigName}.
 *
 * <p>Multiple {@link ConfigValue} objects can be associated with a {@link ConfigName}.
 * To distinguish between these the {@link ConfigKey} can be used.
 *
 * <p>For example, the DelimitedTextParser requires the output fields to be
 * defined.  In this case, two {@link ConfigValue}s are required; one to identify the
 * label of the output field and another for the index.
 */
public class ConfigValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Regex validValue = Regex.of("^.{1,200}$");
    private String value;

    /**
     * Creates a {@link ConfigValue} with a key and value.
     * @param value The value.
     */
    public static ConfigValue of(String value) {
        return new ConfigValue(value);
    }

    /**
     * Private constructor.  See {@link ConfigValue#of(String)}.
     */
    private ConfigValue(String value) {
        mustMatch(value, validValue, "config value");
        this.value = value;
    }

    public String get() {
        return value;
    }

    @Override
    public String toString() {
        return "ConfigValue{" +
                "value='" + value + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigValue that = (ConfigValue) o;
        return new EqualsBuilder()
                .append(value, that.value)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(value)
                .toHashCode();
    }
}
