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

package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.StringFieldValue;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class RemoveFieldParserTest {

    @Test
    void removeField() {
        Message input = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .addField(FieldName.of("field2"), StringFieldValue.of("value2"))
                .addField(FieldName.of("field3"), StringFieldValue.of("value3"))
                .build();
        Message output = new RemoveFieldParser()
                .removeField(FieldName.of("field1"))
                .removeField(FieldName.of("field2"))
                .parse(input);

        assertFalse(output.getField(FieldName.of("field1")).isPresent(), 
            "Expected 'field1' to have been removed.");
        assertFalse(output.getField(FieldName.of("field2")).isPresent(), 
            "Expected 'field2' to have been removed.");
        assertEquals(StringFieldValue.of("value3"), output.getField(FieldName.of("field3")).get(),
            "Expected 'field3' to remain.");
    }

    @Test
    void nothingToRemove() {
        Message input = Message.builder()
                .addField(FieldName.of("field1"), StringFieldValue.of("value1"))
                .addField(FieldName.of("field2"), StringFieldValue.of("value2"))
                .addField(FieldName.of("field3"), StringFieldValue.of("value3"))
                .build();
        Message output = new RemoveFieldParser()
                .parse(input);

        assertEquals(StringFieldValue.of("value1"), output.getField(FieldName.of("field1")).get(),
                "Expected 'field1' to remain.");
        assertEquals(StringFieldValue.of("value2"), output.getField(FieldName.of("field2")).get(),
                "Expected 'field2' to remain.");
        assertEquals(StringFieldValue.of("value3"), output.getField(FieldName.of("field3")).get(),
                "Expected 'field3' to remain.");
    }

    @Test
    void configure() {
        RemoveFieldParser parser = new RemoveFieldParser();
        parser.removeField("field1");
        assertThat(parser.getFieldsToRemove(), hasItems(FieldName.of("field1")));
    }
}
