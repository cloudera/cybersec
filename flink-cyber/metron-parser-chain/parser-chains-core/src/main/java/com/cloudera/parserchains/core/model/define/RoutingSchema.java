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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
        return "RoutingSchema{"
               + "matchingField='" + matchingField + '\''
               + ", routes=" + routes
               + '}';
    }
}
