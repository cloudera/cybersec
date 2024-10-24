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

package com.cloudera.parserchains.core.model.config;

import static com.cloudera.parserchains.core.Validator.mustMatch;

import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.Regex;
import java.io.Serializable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The name of a configuration parameter used to configure a {@link Parser}.
 */
public class ConfigName implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Regex validName = Regex.of("^[a-zA-Z_0-9 ,-.:()]{1,40}$");
    private final String name;

    /**
     * Create a {@link ConfigName}.
     *
     * @param name The name of the configuration parameter.
     */
    public static ConfigName of(String name) {
        return new ConfigName(name);
    }

    /**
     * Private constructor. See {@link ConfigName#of(String)}.
     */
    private ConfigName(String name) {
        mustMatch(name, validName, "name");
        this.name = name;
    }

    public String get() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConfigName that = (ConfigName) o;
        return new EqualsBuilder()
              .append(name, that.name)
              .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
              .append(name)
              .toHashCode();
    }

    @Override
    public String toString() {
        return "ConfigName{"
               + "name='" + name + '\''
               + '}';
    }
}
