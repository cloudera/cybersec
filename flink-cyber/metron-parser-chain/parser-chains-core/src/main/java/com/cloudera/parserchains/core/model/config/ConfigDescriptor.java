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

import com.cloudera.parserchains.core.Parser;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Fully describes a parameter that is used to configure a {@link Parser}.
 */
public class ConfigDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The unique name of the configuration parameter.
     *
     * <p>This is used to identify the parameter and is not displayed to the user.
     *
     * <p>For example, 'fieldToRename', 'inputField', or 'outputField'.
     */
    private final ConfigName name;

    /**
     * A description of the configuration parameter.
     *
     * <p>This description is displayed to the user.
     */
    private final ConfigDescription description;

    /**
     * Defines if the user is required to define a value for this configuration parameter.
     *
     * <p>If true, a value must be provided by the user.  Otherwise, false.
     */
    private final boolean required;

    /**
     * Each configuration parameter can accept multiple, keyed values. This defines
     * those accepted values.
     *
     * <p>For example, a configuration parameter used to define a field to rename
     * will require two, keyed values.  It requires a "from" value and a "to" value so that
     * it can rename the field named "from" to "to".
     */
    private final List<ConfigKey> acceptedValues;

    /**
     * When multiple values are defined by the user, is the effective cumulative or
     * cancellative?
     *
     * <p>For example, it does not make sense for a user to define multiple "input field"
     * values.  The multiple values would simply overwrite one another.  In other cases,
     * like defining multiple fields to remove, the effect is cumulative.  Defining multiple
     * field to remove is acceptable.
     */
    private final boolean cumulative;

    /**
     * A builder that can be used to construct Creates a {@link ConfigDescriptor}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Private constructor. See ConfigDescriptor{@link #builder()}.
     */
    private ConfigDescriptor(ConfigName name,
                             ConfigDescription description,
                             boolean required,
                             List<ConfigKey> acceptedValues,
                             boolean cumulative) {
        this.name = Objects.requireNonNull(name, "Name is required.");
        this.description = Objects.requireNonNull(description, "Description is required.");
        this.required = required;
        this.acceptedValues = Objects.requireNonNull(acceptedValues, "Values are required");
        this.cumulative = cumulative;
    }

    public ConfigName getName() {
        return name;
    }

    public ConfigDescription getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public List<ConfigKey> getAcceptedValues() {
        return Collections.unmodifiableList(acceptedValues);
    }

    public boolean isCumulative() {
        return cumulative;
    }

    public static class Builder {
        private ConfigName name;
        private ConfigDescription description;
        private boolean required;
        private final List<ConfigKey> acceptedValues;
        private boolean cumulative;

        public Builder() {
            this.acceptedValues = new ArrayList<>();
        }

        public Builder name(ConfigName name) {
            this.name = Objects.requireNonNull(name, "A name is required.");
            return this;
        }

        public Builder name(String name) {
            return name(ConfigName.of(name));
        }

        public Builder description(ConfigDescription description) {
            this.description = Objects.requireNonNull(description, "A description is required.");
            return this;
        }

        public Builder description(String description) {
            return description(ConfigDescription.of(description));
        }

        public Builder isRequired(boolean isRequired) {
            this.required = isRequired;
            return this;
        }

        public Builder acceptsValue(ConfigKey key) {
            this.acceptedValues.add(key);
            return this;
        }

        public Builder acceptsValue(String key, String label, String description) {
            ConfigKey configKey = ConfigKey.builder()
                                           .key(key)
                                           .label(label)
                                           .description(description)
                                           .build();
            return acceptsValue(configKey);
        }

        public Builder isCumulative(boolean cumulative) {
            this.cumulative = cumulative;
            return this;
        }

        public ConfigDescriptor build() {
            if (acceptedValues.size() == 0) {
                throw new IllegalArgumentException("Must define at least 1 required value.");
            }

            // shortcut - if name not defined, use the name associated with the ConfigKey
            if (name == null && acceptedValues.size() > 0) {
                name = ConfigName.of(acceptedValues.get(0).getKey());
            }

            // shortcut - if description not defined, use the name associated with the ConfigKey
            if (description == null && acceptedValues.size() > 0) {
                description = acceptedValues.get(0).getDescription();
            }

            return new ConfigDescriptor(name, description, required, acceptedValues, cumulative);
        }
    }
}
