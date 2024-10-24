/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.parserchains.queryservice.model.summary;

import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.model.define.ParserName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Describes a type of parser available to the user for
 * constructing a parser chain.
 */
public class ParserSummary {

    @JsonProperty("id")
    private ParserID id;

    @JsonProperty("name")
    private ParserName name;

    @JsonIgnore
    private String description;

    public String getDescription() {
        return description;
    }

    public ParserSummary setDescription(String description) {
        this.description = description;
        return this;
    }

    public ParserID getId() {
        return id;
    }

    public ParserSummary setId(ParserID id) {
        this.id = id;
        return this;
    }

    public ParserName getName() {
        return name;
    }

    public ParserSummary setName(ParserName name) {
        this.name = name;
        return this;
    }

    public ParserSummary setName(String name) {
        return setName(ParserName.of(name));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ParserSummary that = (ParserSummary) o;
        return Objects.equals(id, that.id)
               && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "ParserType{"
               + "id=" + id
               + ", name=" + name
               + '}';
    }
}
