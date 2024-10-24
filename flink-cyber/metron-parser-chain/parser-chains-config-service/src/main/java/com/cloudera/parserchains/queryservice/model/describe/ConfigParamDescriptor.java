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

package com.cloudera.parserchains.queryservice.model.describe;

import com.cloudera.parserchains.core.catalog.WidgetType;
import com.cloudera.parserchains.core.model.define.ConfigValueSchema;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Describes a configuration parameter accepted by a parser.
 */
public class ConfigParamDescriptor {

    /**
     * The unique name of the configuration parameter.  This value
     * is used only to identify the parameter and is not displayed
     * to the user.
     */
    private String name;

    /**
     * Defines the type of widget presented to the user when
     * configuring the parameter.
     */
    private WidgetType type;
    private static final WidgetType DEFAULT_WIDGET_TYPE = WidgetType.TEXT;

    /**
     * A label for the parameter that is displayed to the user.
     */
    private String label;

    /**
     * A description of the parameter that is displayed to the user.
     */
    private String description;

    /**
     * Defines whether the user is required to define a value for this configuration parameter.
     */
    private boolean required;

    /**
     * Defines whether the user is able to enter multiple values per parameter.
     */
    private boolean multipleValues;

    /**
     * Specifies if the parameter specifies the output field name.
     * If true, the selection will be provided with possible field names.
     *
     * <p>This value is optional.
     */
    private boolean isOutputName;

    /**
     * Defines a path that allows the UI to organize the parameters.
     *
     * <p>In cases where multiple, associated values are accepted, like a field rename with a
     * "from" and "to" value, the associated values should use the same path like "config.fieldToRename".
     *
     * <p>The UI expects the root of all paths to be "config".
     */
    private String path;
    private static final String DEFAULT_PATH = "config";

    /**
     * Should the user be allowed to enter multiple values for this parameter?.
     */
    private boolean multiple;
    private static final boolean DEFAULT_MULTIPLE = false;

    /**
     * The default value used if none other is provided.
     */
    private List<ConfigValueSchema> defaultValue;

    public ConfigParamDescriptor() {
        this.path = DEFAULT_PATH;
        this.multiple = DEFAULT_MULTIPLE;
        this.type = DEFAULT_WIDGET_TYPE;
    }

    public String getName() {
        return name;
    }

    public ConfigParamDescriptor setName(String name) {
        this.name = name;
        return this;
    }

    public WidgetType getType() {
        return type;
    }

    public ConfigParamDescriptor setType(WidgetType type) {
        this.type = type;
        return this;
    }

    public String getLabel() {
        return label;
    }

    public ConfigParamDescriptor setLabel(String label) {
        this.label = label;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ConfigParamDescriptor setDescription(String description) {
        this.description = description;
        return this;
    }

    public boolean isRequired() {
        return required;
    }

    public ConfigParamDescriptor setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public boolean isMultipleValues() {
        return multipleValues;
    }

    public ConfigParamDescriptor setMultipleValues(boolean multipleValues) {
        this.multipleValues = multipleValues;
        return this;
    }

    public boolean isOutputName() {
        return isOutputName;
    }

    public ConfigParamDescriptor setOutputName(boolean outputName) {
        isOutputName = outputName;
        return this;
    }

    public String getPath() {
        return path;
    }

    public ConfigParamDescriptor setPath(String path) {
        this.path = path;
        return this;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public ConfigParamDescriptor setMultiple(boolean multiple) {
        this.multiple = multiple;
        return this;
    }

    public List<ConfigValueSchema> getDefaultValue() {
        return defaultValue;
    }

    public ConfigParamDescriptor addDefaultValue(String name, String value) {
        // the front-end requires a list of values. the list will only ever contain a single, default value
        this.defaultValue = Collections.singletonList(new ConfigValueSchema().addValue(name, value));
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
        ConfigParamDescriptor that = (ConfigParamDescriptor) o;
        return required == that.required
               && multiple == that.multiple
               && Objects.equals(name, that.name)
               && Objects.equals(type, that.type)
               && Objects.equals(label, that.label)
               && Objects.equals(description, that.description)
               && Objects.equals(path, that.path)
               && Objects.equals(defaultValue, that.defaultValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, label, description, required, path, multiple, defaultValue);
    }

    @Override
    public String toString() {
        return "ConfigParamDescriptor{"
               + "name='" + name + '\''
               + ", type='" + type + '\''
               + ", label='" + label + '\''
               + ", description='" + description + '\''
               + ", required=" + required
               + ", path='" + path + '\''
               + ", multiple=" + multiple
               + ", defaultValue='" + defaultValue + '\''
               + '}';
    }
}
