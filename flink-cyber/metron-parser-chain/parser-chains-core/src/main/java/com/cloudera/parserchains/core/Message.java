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

package com.cloudera.parserchains.core;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * A {@link Message} is consumed and parsed by a {@link Parser}.
 *
 * <p>
 * A {@link Message} is composed of a collection of fields. The message fields
 * are represented as ({@link FieldName}, {@link FieldValue}) pairs.
 *
 * <p>
 * A {@link Message} is immutable and a {@link Builder} should be used to
 * construct one.
 */
@EqualsAndHashCode
@ToString
public class Message {

    /**
     * Constructs a {@link Message}.
     */
    public static class Builder {
        private final Map<FieldName, FieldValue> fields;
        private Throwable error;
        private LinkName createdBy;
        private boolean emit = true;

        public Builder() {
            this.fields = new HashMap<>();
        }

        /**
         * Adds all fields from a {@link Message}.
         *
         * @param message The message to copy fields from.
         */
        public Builder withFields(Message message) {
            Objects.requireNonNull(message, "A message is required.");
            this.fields.putAll(message.fields);
            return this;
        }

        /**
         * Clones a message by copying all underlying fields.
         *
         * @param message The message to clone.
         */
        public Builder clone(Message message) {
            Objects.requireNonNull(message, "A message to clone is required.");
            this.fields.putAll(message.fields);
            this.error = message.error;
            this.createdBy = message.createdBy;
            return this;
        }

        /**
         * Add a field to the message.
         *
         * @param name  The name of the field to add.
         * @param value The value of the field to add.
         */
        public Builder addField(FieldName name, FieldValue value) {
            this.fields.put(
                  Objects.requireNonNull(name, "A valid field name is required."),
                  Objects.requireNonNull(value, "A valid field value is required."));
            return this;
        }

        /**
         * Add a field to the message.
         *
         * @param name  The name of the field to add.
         * @param value The value of the field to add.
         */
        public Builder addField(FieldName name, String value) {
            return addField(name, StringFieldValue.of(value));
        }

        /**
         * Add a field to the message.
         *
         * @param name  The name of the field to add.
         * @param value The value of the field to add.
         */
        public Builder addField(String name, String value) {
            return addField(FieldName.of(name), StringFieldValue.of(value));
        }

        /**
         * Remove a field from the message.
         *
         * @param name The name of the field to remove.
         */
        public Builder removeField(FieldName name) {
            this.fields.remove(Objects.requireNonNull(name, "The name of the field to remove is required."));
            return this;
        }

        /**
         * Removes multiple fields from the message.
         *
         * @param fieldNames The name of the fields to remove.
         */
        public Builder removeFields(List<FieldName> fieldNames) {
            for (FieldName fieldName : fieldNames) {
                this.fields.remove(Objects.requireNonNull(fieldName, "The name of the field to remove is required."));
            }
            return this;
        }

        /**
         * Renames a field, if the field exists within the message. If the
         * field does not exist, no action taken.
         *
         * @param from The original field name.
         * @param to   The new field name.
         */
        public Builder renameField(FieldName from, FieldName to) {
            if (fields.containsKey(from)) {
                FieldValue value = fields.remove(from);
                fields.put(to, value);
            }
            return this;
        }

        /**
         * Adds an error to the message. This indicates that an error
         * occurred while parsing.
         *
         * @param error The error that occurred.
         */
        public Builder withError(Throwable error) {
            this.error = Objects.requireNonNull(error, "An error is required.");
            return this;
        }

        /**
         * Adds an error to the message. This indicates that an error
         * occurred while parsing.
         *
         * @param message The error message.
         */
        public Builder withError(String message) {
            this.error = new IllegalStateException(Objects.requireNonNull(message, "An error message is required."));
            return this;
        }

        /**
         * Adds an error to the message to indicate that an error occurred
         * while parsing.
         *
         * @param message   The error message.
         * @param rootCause The root cause exception.
         */
        public Builder withError(String message, Throwable rootCause) {
            this.error = new RuntimeException(message, rootCause);
            return this;
        }

        /**
         * Assigns a {@link LinkName} to this message indicating which link in the
         * chain was responsible for creating the message.
         *
         * @param createdBy The name of the link that created this message.
         */
        public Builder createdBy(LinkName createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder emit(boolean emit) {
            this.emit = emit;
            return this;
        }

        /**
         * Builds a {@link Message}.
         *
         * @return The message.
         */
        public Message build() {
            return new Message(this);
        }
    }

    private final Map<FieldName, FieldValue> fields;
    private final Throwable error;
    private final LinkName createdBy;
    private final boolean emit;

    private Message(Builder builder) {
        this.fields = new HashMap<>();
        this.fields.putAll(builder.fields);
        this.error = builder.error;
        this.createdBy = builder.createdBy;
        this.emit = builder.emit;
    }

    /**
     * Builder method.
     *
     * @return A {@link Builder} that can be used to create a message.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the value of a field within this message.
     *
     * @param fieldName The name of the field.
     * @return The value of the field or Optional.empty if it does not exist.
     */
    public Optional<FieldValue> getField(FieldName fieldName) {
        if (fields.containsKey(fieldName)) {
            return Optional.of(fields.get(fieldName));
        } else {
            return Optional.empty();
        }
    }

    public Map<FieldName, FieldValue> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    public Optional<Throwable> getError() {
        return Optional.ofNullable(error);
    }

    public LinkName getCreatedBy() {
        return createdBy;
    }

    public boolean getEmit() {
        return emit;
    }
}
