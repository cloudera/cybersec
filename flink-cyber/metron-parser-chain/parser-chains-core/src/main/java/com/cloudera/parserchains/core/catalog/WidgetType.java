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
