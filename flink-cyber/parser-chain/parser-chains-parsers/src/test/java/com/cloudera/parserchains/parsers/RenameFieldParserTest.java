package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class RenameFieldParserTest {

    @Test
    void renameField() {
        // rename 'original1' to 'new1'
        Message input = Message.builder()
                .addField(FieldName.of("original1"), FieldValue.of("value1"))
                .addField(FieldName.of("original2"), FieldValue.of("value2"))
                .build();
        Message output = new RenameFieldParser()
                .renameField(FieldName.of("original1"), FieldName.of("new1"))
                .parse(input);

        assertEquals(2, output.getFields().size(), 
            "Expected 2 output fields in the message.");
        assertFalse(output.getField(FieldName.of("original1")).isPresent(), 
            "Expected 'original1' to have been renamed.");
        assertEquals(FieldValue.of("value1"), output.getField(FieldName.of("new1")).get(), 
            "Expected 'original1' to have been renamed to 'new1'.");
        assertEquals(FieldValue.of("value2"), output.getField(FieldName.of("original2")).get(), 
            "Expected 'original2' to remain unchanged.");
    }

    @Test
    void renameFieldDoesNotExist() {
        Message input = Message.builder()
                .addField(FieldName.of("original1"), FieldValue.of("value1"))
                .build();
        Message output = new RenameFieldParser()
                .renameField(FieldName.of("doesNotExist"), FieldName.of("new1"))
                .parse(input);
        assertEquals(input, output,
                "The output fields should be the same as the input. No rename occurred.");
    }

    @Test
    void configure() {
        RenameFieldParser parser = new RenameFieldParser();
        parser.renameField("original1", "new1");
        assertEquals(FieldName.of("new1"), parser.getFieldsToRename().get(FieldName.of("original1")));
    }
}
