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

package org.apache.metron.common.configuration;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a group of sensors.  Sensor parser groups are used to execute parsers in a single execution context (Storm
 * bolt for example).
 */
public class SensorParserGroup {

    private String name;
    private String description;
    private Set<String> sensors = new HashSet<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getSensors() {
        return sensors;
    }

    public void setSensors(Set<String> sensors) {
        this.sensors = sensors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SensorParserGroup that = (SensorParserGroup) o;
        return Objects.equals(name, that.name)
               && Objects.equals(description, that.description)
               && Objects.equals(sensors, that.sensors);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name, description, sensors);
    }

    @Override
    public String toString() {
        return "SensorParserGroup{"
               + "name='" + name + '\''
               + ", description='" + description + '\''
               + ", sensors=" + sensors
               + '}';
    }
}
