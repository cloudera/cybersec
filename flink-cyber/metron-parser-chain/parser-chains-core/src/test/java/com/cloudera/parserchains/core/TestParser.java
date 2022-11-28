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

import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.catalog.Parameter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@MessageParser(
        name = "Test Parser",
        description =  "This parser is used for testing only."
)
public class TestParser implements Parser {
    private FieldName inputField;
    private Map<FieldName, Integer> outputFields;

    public TestParser() {
        outputFields = new HashMap<>();
    }

    @Override
    public Message parse(Message message) {
        return message;
    }

    public TestParser withOutputField(FieldName fieldName, int index) {
        outputFields.put(fieldName, index);
        return this;
    }

    public Map<FieldName, Integer> getOutputFields() {
        return outputFields;
    }

    /**
     * @param inputField The name of the field containing the text to parse.
     */
    public TestParser withInputField(FieldName inputField) {
        this.inputField = Objects.requireNonNull(inputField);
        return this;
    }

    public FieldName getInputField() {
        return inputField;
    }

    @Configurable(key="inputField", label="Input Field", description="The name of the field to parse.")
    public TestParser withInputField(String inputField) {
        withInputField(FieldName.of(inputField));
        return this;
    }

    @Configurable(key="outputField", label="Output Field", description="The output field to create.")
    public TestParser withOutputField(
            @Parameter(key="fieldName", label="Field Name") String fieldName,
            @Parameter(key="columnIndex", label="Column Index") String fieldIndex) {
        return withOutputField(FieldName.of(fieldName), Integer.parseInt(fieldIndex));
    }
}
