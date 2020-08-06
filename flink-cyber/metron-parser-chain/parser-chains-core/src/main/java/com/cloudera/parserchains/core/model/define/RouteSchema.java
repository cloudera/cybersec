package com.cloudera.parserchains.core.model.define;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * Describes the structure of one route.
 */
@JsonPropertyOrder({"id", "name", "matchingValue", "default", "subchain"})
public class RouteSchema implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * If the field value matches this regular expression, this route is taken.
     *
     * <p>This value is not used for the default route.
     */
    @JsonProperty("matchingValue")
    private String matchingValue;

    /**
     * The default route is taken if there is no match to another route.
     *
     * <p>Only one route should be defined as the default route.
     *
     * <p>The {@link #matchingValue} field is not used for the default route.
     */
    @JsonProperty("default")
    private boolean isDefault = false;

    /**
     * The subchain for this route.
     */
    @JsonProperty("subchain")
    private ParserChainSchema subChain;

    @JsonProperty("id")
    private String label;

    @JsonProperty("name")
    private ParserName name;

    public String getMatchingValue() {
        return matchingValue;
    }

    public RouteSchema setMatchingValue(String matchingValue) {
        this.matchingValue = matchingValue;
        return this;
    }

    public ParserChainSchema getSubChain() {
        return subChain;
    }

    public RouteSchema setSubChain(ParserChainSchema subChain) {
        this.subChain = subChain;
        return this;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public RouteSchema setDefault(boolean aDefault) {
        isDefault = aDefault;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public RouteSchema setLabel(String label) {
        this.label = label;
        return this;
    }

    public ParserName getName() {
        return name;
    }

    public RouteSchema setName(ParserName name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RouteSchema that = (RouteSchema) o;
        return new EqualsBuilder()
                .append(isDefault, that.isDefault)
                .append(matchingValue, that.matchingValue)
                .append(subChain, that.subChain)
                .append(label, that.label)
                .append(name, that.name)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(matchingValue)
                .append(isDefault)
                .append(subChain)
                .append(label)
                .append(name)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "RouteSchema{" +
                "matchingValue='" + matchingValue + '\'' +
                ", isDefault=" + isDefault +
                ", subChain=" + subChain +
                ", label='" + label + '\'' +
                ", name=" + name +
                '}';
    }
}
