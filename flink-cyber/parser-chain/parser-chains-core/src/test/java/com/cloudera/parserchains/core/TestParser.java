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
