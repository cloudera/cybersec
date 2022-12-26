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

package com.cloudera.parserchains.queryservice.model.describe;

import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.model.define.ParserName;
import com.cyber.jackson.annotation.JsonProperty;
import com.cyber.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes features of a parser including its configuration parameters.
 *
 * <p>This defines the data model for how a parser is described so that it
 * can be viewed and configured in the front-end.
 */
@JsonPropertyOrder({"id", "name", "schemaItems"})
public class ParserDescriptor {

  /**
   * The id of the parser.
   *
   * <p>This id should be unique amongst all of the available parsers.  This is
   * derived from the name of the class implementing the parser.
   */
  @JsonProperty("id")
  private ParserID parserID;

  /**
   * The name of the parser.  This is a descriptive name provided by
   * the parser author and is not guaranteed to be unique.
   */
  @JsonProperty("name")
  private ParserName parserName;

  /**
   * Describes the configuration parameters accepted by this parser.
   */
  @JsonProperty("schemaItems")
  private List<ConfigParamDescriptor> configurations;

  public ParserDescriptor() {
    configurations = new ArrayList<>();
  }

  public ParserID getParserID() {
    return parserID;
  }

  public ParserDescriptor setParserID(ParserID parserID) {
    this.parserID = parserID;
    return this;
  }

  public ParserName getParserName() {
    return parserName;
  }

  public ParserDescriptor setParserName(ParserName parserName) {
    this.parserName = parserName;
    return this;
  }

  public List<ConfigParamDescriptor> getConfigurations() {
    return configurations;
  }

  public ParserDescriptor setConfigurations(List<ConfigParamDescriptor> configurations) {
    this.configurations = configurations;
    return this;
  }

  public ParserDescriptor addConfiguration(ConfigParamDescriptor item) {
    configurations.add(item);
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
    ParserDescriptor that = (ParserDescriptor) o;
    return new EqualsBuilder()
            .append(parserID, that.parserID)
            .append(parserName, that.parserName)
            .append(configurations, that.configurations)
            .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(parserID)
            .append(parserName)
            .append(configurations)
            .toHashCode();
  }
}
