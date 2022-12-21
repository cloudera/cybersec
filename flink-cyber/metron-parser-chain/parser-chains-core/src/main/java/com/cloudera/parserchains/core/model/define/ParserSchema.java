/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.parserchains.core.model.define;

import com.cyber.jackson.annotation.JsonProperty;
import com.cyber.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes the structure of one parser within a {@link ParserChainSchema}.
 */
@JsonPropertyOrder({"id", "name", "type", "config"})
public class ParserSchema implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * A label given to this parser that is generated by the front-end.
   *
   * <p>This is only used by the front-end and allows errors encountered
   * in the Live View to be mapped to the specific parser that failed.
   */
  @JsonProperty("id")
  private String label;

  /**
   * The id of the parser.
   *
   * <p>This id should be unique amongst all of the available parsers.  This is
   * derived from the name of the class implementing the parser.
   *
   * <p>The front-end refers to this as the "type" of parser, which might be a
   * better name than id.
   */
  @JsonProperty("type")
  private ParserID id;

  /**
   * A name given to this parser by the user.
   */
  @JsonProperty("name")
  private ParserName name;

  /**
   * Defines how the user has configured this parser.
   */
  @JsonProperty("config")
  private Map<String, List<ConfigValueSchema>> config;

  /**
   * If the user has created a router, this describes the routing features
   * as configured by the user.
   */
  @JsonProperty("routing")
  private RoutingSchema routing;

  public ParserSchema() {
    config = new HashMap<>();
  }

  public ParserID getId() {
    return id;
  }

  public ParserSchema setId(ParserID id) {
    this.id = id;
    return this;
  }

  public ParserName getName() {
    return name;
  }

  public ParserSchema setName(ParserName name) {
    this.name = name;
    return this;
  }

  public String getLabel() {
    return label;
  }

  public ParserSchema setLabel(String label) {
    this.label = label;
    return this;
  }

  public Map<String, List<ConfigValueSchema>> getConfig() {
    return config;
  }

  public ParserSchema setConfig(Map<String, List<ConfigValueSchema>> config) {
    this.config = config;
    return this;
  }

  public ParserSchema addConfig(String key, ConfigValueSchema value) {
    List<ConfigValueSchema> values;
    if(config.containsKey(key)) {
      values = config.get(key);
    } else {
      values = new ArrayList<>();
      config.put(key, values);
    }
    values.add(value);
    return this;
  }

  public RoutingSchema getRouting() {
    return routing;
  }

  public ParserSchema setRouting(RoutingSchema routing) {
    this.routing = routing;
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
    ParserSchema that = (ParserSchema) o;
    return new EqualsBuilder()
            .append(label, that.label)
            .append(id, that.id)
            .append(name, that.name)
            .append(config, that.config)
            .append(routing, that.routing)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(label)
            .append(id)
            .append(name)
            .append(config)
            .append(routing)
            .toHashCode();
  }

  @Override
  public String toString() {
    return "ParserSchema{" +
            "label='" + label + '\'' +
            ", id=" + id +
            ", name=" + name +
            ", config=" + config +
            ", routing=" + routing +
            '}';
  }
}
