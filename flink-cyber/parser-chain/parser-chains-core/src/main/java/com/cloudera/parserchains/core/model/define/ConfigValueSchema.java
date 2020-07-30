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
