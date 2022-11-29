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

package com.cloudera.parserchains.core.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Defines the valid types of widgets for the {@link Configurable#widgetType()}
 * and {@link Parameter#widgetType()}.
 */
public enum WidgetType {
    /**
     * A simple one-line text box.
     */
    TEXT("text"),

    /**
     * A multiline text input area.
     */
    TEXTAREA("textarea");

    private final String type;

    WidgetType(String type) {
        this.type = type;
    }

    @JsonCreator
    public WidgetType of(String type) {
        return WidgetType.valueOf(type.toUpperCase());
    }

    @JsonValue
    public String getType() {
        return type;
    }
}
