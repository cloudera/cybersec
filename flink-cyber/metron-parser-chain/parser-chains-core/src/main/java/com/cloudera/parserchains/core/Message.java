package com.cloudera.parserchains.core;


import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;

/**
 * A {@link Message} is consumed and parsed by a {@link Parser}.
 *
 * A {@link Message} is composed of a collection of fields. The message fields
 * are represented as ({@link FieldName}, {@link FieldValue}) pairs.
 *
 * A {@link Message} is immutable and a {@link Builder} should be used to
 * construct one.
 */
public class Message {

    /**
     * Constructs a {@link Message}.
     */
    public static class Builder {
        private Map<FieldName, FieldValue> fields;
        private Throwable error;
        private LinkName createdBy;

        public Builder() {
            this.fields = new HashMap<>();
        }

        /**
         * Adds all fields from a {@link Message}.
         * @param message The message to copy fields from.
         * @return
         */
        public Builder withFields(Message message) {
            Objects.requireNonNull(message, "A message is required.");
            this.fields.putAll(message.fields);
            return this;
        }

        /**
         * Clones a message by copying all underlying fields.
         * @param message The message to clone.
         * @return
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
         * @param name The name of the field to add.
         * @param value The value of the field to add.
         * @return
         */
        public Builder addField(FieldName name, FieldValue value) {
            this.fields.put(
                    Objects.requireNonNull(name, "A valid field name is required."),
                    Objects.requireNonNull(value, "A valid field value is required."));
            return this;
        }

        /**
         * Add a field to the message.
         * @param name The name of the field to add.
         * @param value The value of the field to add.
         * @return
         */
        public Builder addField(FieldName name, String value) {
            return addField(name, FieldValue.of(value));
        }

        /**
         * Add a field to the message.
         * @param name The name of the field to add.
         * @param value The value of the field to add.
         * @return
         */
        public Builder addField(String name, String value) {
            return addField(FieldName.of(name), FieldValue.of(value));
        }

        /**
         * Remove a field from the message.
         * @param name The name of the field to remove.
         * @return
         */
        public Builder removeField(FieldName name) {
            this.fields.remove(Objects.requireNonNull(name, "The name of the field to remove is required."));
            return this;
        }

        /**
         * Removes multiple fields from the message.
         * @param fieldNames The name of the fields to remove.
         * @return
         */
        public Builder removeFields(List<FieldName> fieldNames) {
            for(FieldName fieldName: fieldNames) {
                this.fields.remove(Objects.requireNonNull(fieldName, "The name of the field to remove is required."));
            }
            return this;
        }

        /**
         * Renames a field, if the field exists within the message. If the
         * field does not exist, no action taken.
         * @param from The original field name.
         * @param to The new field name.
         * @return
         */
        public Builder renameField(FieldName from, FieldName to) {
            if(fields.containsKey(from)) {
                FieldValue value = fields.remove(from);
                fields.put(to, value);
            }
            return this;
        }

        /**
         * Adds an error to the message. This indicates that an error
         * occurred while parsing.
         * @param error The error that occurred.
         * @return
         */
        public Builder withError(Throwable error) {
            this.error = Objects.requireNonNull(error, "An error is required.");
            return this;
        }

        /**
         * Adds an error to the message. This indicates that an error
         * occurred while parsing.
         * @param message The error message.
         * @return
         */
        public Builder withError(String message) {
            this.error = new IllegalStateException(Objects.requireNonNull(message, "An error message is required."));
            return this;
        }

        /**
         * Adds an error to the message to indicate that an error occurred
         * while parsing.
         * @param message The error message.
         * @param rootCause The root cause exception.
         * @return
         */
        public Builder withError(String message, Throwable rootCause) {
            this.error = new RuntimeException(message, rootCause);
            return this;
        }

        /**
         * Assigns a {@link LinkName} to this message indicating which link in the
         * chain was responsible for creating the message.
         * @param createdBy The name of the link that created this message.
         * @return
         */
        public Builder createdBy(LinkName createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        /**
         * Builds a {@link Message}.
         * @return The message.
         */
        public Message build() {
            return new Message(this);
        }
    }

    private Map<FieldName, FieldValue> fields;
    private Throwable error;
    private LinkName createdBy;

    private Message(Builder builder) {
        this.fields = new HashMap<>();
        this.fields.putAll(builder.fields);
        this.error = builder.error;
        this.createdBy = builder.createdBy;
    }

    /**
     * @return A {@link Builder} that can be used to create a message.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the value of a field within this message.
     * @param fieldName The name of the field.
     * @return The value of the field or Optional.empty if it does not exist.
     */
    public Optional<FieldValue> getField(FieldName fieldName) {
        if(fields.containsKey(fieldName)) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Message message = (Message) o;
        return new EqualsBuilder()
                .append(fields, message.fields)
                .append(error, message.error)
                .append(createdBy, message.createdBy)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(fields)
                .append(error)
                .append(createdBy)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "Message{" +
                "fields=" + fields +
                ", error=" + error +
                ", createdBy=" + createdBy +
                '}';
    }
}
