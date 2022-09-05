package com.cloudera.parserchains.core;

import com.cloudera.parserchains.core.model.define.ParserName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class MessageTest {
    static ParserName parserName = ParserName.of("Some Test Parser");
    static LinkName createdBy = LinkName.of("parser22", parserName);

    @Test
    void addField() {
        Message message = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .addField(FieldName.of("field2"), StringFieldValue.of("value2"))
                .addField(FieldName.of("field3"), StringFieldValue.of("value3"))
                .createdBy(createdBy)
                .build();

        assertEquals(3, message.getFields().size(), 
            "Expected 3 fields to have been added to the message.");
        assertEquals(StringFieldValue.of("value1"), message.getField(FieldName.of("field1")).get(),
            "Expected field1 to have been added to the message.");
        assertEquals(StringFieldValue.of("value2"), message.getField(FieldName.of("field2")).get(),
            "Expected field2 to have been added to the message.");
        assertEquals(StringFieldValue.of("value3"), message.getField(FieldName.of("field3")).get(),
            "Expected field3 to have been added to the message.");
        assertFalse(message.getError().isPresent(),
            "Expected no errors to have been attached to the message.");
    }

    @Test
    void removeField() {
        Message original = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .addField(FieldName.of("field2"), StringFieldValue.of("value2"))
                .addField(FieldName.of("field3"), StringFieldValue.of("value3"))
                .createdBy(createdBy)
                .build();
        Message copy = Message.builder()
                .withFields(original)
                .removeField(FieldName.of("field1"))
                .createdBy(createdBy)
                .build();
        
        assertFalse(copy.getField(FieldName.of("field1")).isPresent(),
            "Expected field1 to have been removed from the message.");
        assertEquals(StringFieldValue.of("value2"), copy.getField(FieldName.of("field2")).get(),
            "Expected field2 to have been added to the message.");
        assertEquals(StringFieldValue.of("value3"), copy.getField(FieldName.of("field3")).get(),
            "Expected field3 to have been added to the message.");
        assertFalse(copy.getError().isPresent(),
            "Expected no errors to have been attached to the message.");
    }

    @Test
    void removeFields() {
        Message original = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .addField(FieldName.of("field2"), StringFieldValue.of("value2"))
                .addField(FieldName.of("field3"), StringFieldValue.of("value3"))
                .createdBy(createdBy)
                .build();
        Message copy = Message.builder()
                .withFields(original)
                .removeFields(Arrays.asList(FieldName.of("field1"), FieldName.of("field2"), FieldName.of("field3")))
                .createdBy(createdBy)
                .build();

        assertFalse(copy.getField(FieldName.of("field1")).isPresent(),
            "Expected field1 to have been removed from the message.");
        assertFalse(copy.getField(FieldName.of("field2")).isPresent(),
            "Expected field2 to have been removed from the message.");
        assertFalse(copy.getField(FieldName.of("field3")).isPresent(),
            "Expected field3 to have been removed from the message.");
        assertFalse(copy.getError().isPresent(),
            "Expected no errors to have been attached to the message.");
    }

    @Test
    void renameFields() {
        Message original = Message.builder()
                .addField(FieldName.of("original1"), StringFieldValue.of("value1"))
                .addField(FieldName.of("original2"), StringFieldValue.of("value2"))
                .addField(FieldName.of("original3"), StringFieldValue.of("value3"))
                .createdBy(createdBy)
                .build();

        // rename 'original1' to 'new1'
        Message actual = Message.builder()
                .withFields(original)
                .renameField(FieldName.of("original1"), FieldName.of("new1"))
                .renameField(FieldName.of("original2"), FieldName.of("new2"))
                .createdBy(createdBy)
                .build();

        assertEquals(3, actual.getFields().size(), 
            "Expected 3 fields in the message.");
        assertEquals(StringFieldValue.of("value1"), actual.getField(FieldName.of("new1")).get(),
            "Expected original1 to have been renamed to new1.");
        assertEquals(StringFieldValue.of("value2"), actual.getField(FieldName.of("new2")).get(),
            "Expected original2 to have been renamed to new2.");
        assertEquals(StringFieldValue.of("value3"), actual.getField(FieldName.of("original3")).get(),
            "Expected original3 to remain unchanged in the message.");
        assertFalse(actual.getError().isPresent(),
            "Expected no errors to have been attached to the message.");
    }

    @Test
    void renameMissingField() {
        Message original = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .createdBy(createdBy)
                .build();
        // rename 'missing1' which does not exist
        Message actual = Message.builder()
                .withFields(original)
                .renameField(FieldName.of("missing1"), FieldName.of("new1"))
                .createdBy(createdBy)
                .build();

        assertEquals(1, actual.getFields().size(), 
            "Expected 1 field in the message.");
        assertEquals(StringFieldValue.of("value1"), actual.getField(FieldName.of("field1")).get(),
            "Expected field1 to remain unchanged in the message.");
        assertFalse(actual.getError().isPresent(),
            "Expected no errors to have been attached to the message.");        
    }

    @Test
    void withFields() {
        Message original = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .addField(FieldName.of("field2"), StringFieldValue.of("value2"))
                .addField(FieldName.of("field3"), StringFieldValue.of("value3"))
                .createdBy(createdBy)
                .build();
        Message copy = Message.builder()
                .withFields(original)
                .createdBy(createdBy)
                .build();

        assertEquals(3, copy.getFields().size(), 
            "Expected 3 fields the copied message.");
        assertEquals(StringFieldValue.of("value1"), copy.getField(FieldName.of("field1")).get(),
            "Expected field1 to have been added to the message.");
        assertEquals(StringFieldValue.of("value2"), copy.getField(FieldName.of("field2")).get(),
            "Expected field2 to have been added to the message.");
        assertEquals(StringFieldValue.of("value3"), copy.getField(FieldName.of("field3")).get(),
            "Expected field3 to have been added to the message.");
        assertFalse(copy.getError().isPresent(),
            "Expected no errors to have been attached to the message.");
    }

    @Test
    void withEmitTrue() {
        testEmit(true);
    }

    @Test
    void withEmitFalse() {
        testEmit(false);
    }

    @Test
    void withEmitDefault() {
        Message message = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .build();
        assertTrue(message.getEmit());
    }
    private void testEmit(boolean expectedEmit) {
        Message message = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .emit(expectedEmit)
                .build();
        assertEquals(expectedEmit, message.getEmit());
    }

    @Test
    void withErrorMessage() {
        final String errorMessage = "this is an error";
        Message original = Message.builder()
                .withError(errorMessage)
                .createdBy(createdBy)
                .build();
        assertEquals(errorMessage, original.getError().get().getMessage(), 
            "Expected an error message to have been attached to the message.");
    }

    @Test
    void withError() {
        final Exception exception = new IllegalStateException("this is an error");
        Message original = Message.builder()
                .withError(exception)
                .createdBy(createdBy)
                .build();
        assertEquals(exception, original.getError().get(),
            "Expected an exception to have been attached to the message.");
    }

    @Test
    void createdBy() {
        LinkName expectedName = LinkName.of("parser22", parserName);
        Message message = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .createdBy(expectedName)
                .build();
        assertEquals(expectedName, message.getCreatedBy());
    }

    @Test
    void testClone() {
        Message original = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .addField(FieldName.of("field2"), StringFieldValue.of("value2"))
                .addField(FieldName.of("field3"), StringFieldValue.of("value3"))
                .createdBy(createdBy)
                .build();
        Message clone = Message.builder()
                .clone(original)
                .build();
        assertEquals(original, clone,
                "Expected the original and the clone to be equal.");
    }
}
