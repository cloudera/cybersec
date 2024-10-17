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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines the structure of a parser chain.
 */
public class ParserChainSchema implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * The id of the parser chain.
   *
   * <p>This value is generated and is expected to be unique amongst
   * all parser chains.
   */
  @JsonProperty("id")
  private String id;

  /**
   * The user provided name of the parser chain.
   */
  @JsonProperty("name")
  private String name;

  /**
   * The base path for any path-related parser configs.
   */
  @JsonIgnore
  private String basePath;

  /**
   * The parsers in this parser chain.
   */
  @JsonProperty("parsers")
  private List<ParserSchema> parsers;

  public ParserChainSchema() {
    parsers = new ArrayList<>();
  }

  public String getId() {
    return id;
  }

  public ParserChainSchema setId(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public ParserChainSchema setName(String name) {
    this.name = name;
    return this;
  }

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public List<ParserSchema> getParsers() {
    return parsers;
  }

  public ParserChainSchema setParsers(List<ParserSchema> parsers) {
    this.parsers = parsers;
    return this;
  }

  public ParserChainSchema addParser(ParserSchema parserSchema) {
    this.parsers.add(parserSchema);
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
    ParserChainSchema that = (ParserChainSchema) o;
    return new EqualsBuilder()
            .append(id, that.id)
            .append(name, that.name)
            .append(parsers, that.parsers)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(id)
            .append(name)
            .append(parsers)
            .toHashCode();
  }

  @Override
  public String toString() {
    return "ParserChainSchema{" +
            "id='" + id + '\'' +
            ", name='" + name + '\'' +
            ", parsers=" + parsers +
            '}';
  }
}
