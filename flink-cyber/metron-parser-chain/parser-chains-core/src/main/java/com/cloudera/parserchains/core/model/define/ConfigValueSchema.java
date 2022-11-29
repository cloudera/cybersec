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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Describes the configuration values defined by the user when building a parser chain.
 */
public class ConfigValueSchema implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The configuration values as key/value pairs.
     */
    private Map<String, String> values;

    public ConfigValueSchema() {
        this.values = new HashMap<>();
    }

    @JsonAnySetter
    public ConfigValueSchema addValue(String key, String value) {
        Objects.requireNonNull(key, "The key must be defined.");
        Objects.requireNonNull(value, "The value must be defined.");
        this.values.put(key, value);
        return this;
    }

    @JsonAnyGetter
    public Map<String, String> getValues() {
        return values;
    }

    public void setValues(Map<String, String> values) {
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigValueSchema that = (ConfigValueSchema) o;
        return new EqualsBuilder()
                .append(values, that.values)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(values)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ConfigValue{" +
                "values=" + values +
                '}';
    }
}
