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

import com.cloudera.parserchains.core.Regex;
import java.io.Serializable;

/**
 * Describes a parser's configuration parameter for a user.
 */
public class ConfigDescription implements Serializable {

    private static final long serialVersionUID = 1L;

    static final Regex validDescription = Regex.of("^[a-zA-Z_0-9 ,-.:;'\"\\+()]{1,80}$");
    private final String description;

    /**
     * Creates a {@link ConfigDescription}.
     *
     * @param desc The description.
     */
    public static ConfigDescription of(String desc) {
        return new ConfigDescription((desc));
    }

    /**
     * See {@link ConfigDescription#of(String)}.
     */
    private ConfigDescription(String description) {
        mustMatch(description, validDescription, "description");
        this.description = description;
    }

    public String get() {
        return description;
    }
}
