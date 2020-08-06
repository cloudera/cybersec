package com.cloudera.parserchains.core.model.define;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines the structure of a router contained within a parser chain.
 *
 * <p>Only a router will contain these fields.
 */
@JsonPropertyOrder({"matchingField", "routes"})
public class RoutingSchema implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The name of a field whose value is used to determine which
     * route should be taken.
     */
    @JsonProperty("matchingField")
    private String matchingField;

    /**
     * The routes that are available to be taken.
     */
    @JsonProperty("routes")
    private List<RouteSchema> routes;

    public RoutingSchema() {
        this.routes = new ArrayList<>();
    }

    public String getMatchingField() {
        return matchingField;
    }

    public RoutingSchema setMatchingField(String matchingField) {
        this.matchingField = matchingField;
        return this;
    }

    public List<RouteSchema> getRoutes() {
        return routes;
    }

    public RoutingSchema setRoutes(List<RouteSchema> routes) {
        this.routes = routes;
        return this;
    }

    public RoutingSchema addRoute(RouteSchema route) {
        this.routes.add(route);
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
        RoutingSchema that = (RoutingSchema) o;
        return new EqualsBuilder()
                .append(matchingField, that.matchingField)
                .append(routes, that.routes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(matchingField)
                .append(routes)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "RoutingSchema{" +
                "matchingField='" + matchingField + '\'' +
                ", routes=" + routes +
                '}';
    }
}
