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

import com.cloudera.parserchains.core.Constants;
import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.Regex;
import com.cloudera.parserchains.core.StringFieldValue;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.catalog.Parameter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;

/**
 * Parses delimited text like CSV.
 */
@MessageParser(
        name = "Delimited Text",
        description = "Parses delimited text like CSV or TSV.")
public class DelimitedTextParser implements Parser {
    private static final String DEFAULT_DELIMITER = ",";
    private static final String DEFAULT_TRIM = "true";

    /**
     * Defines an output field that is created by the parser.
     */
    static class OutputField {
        FieldName fieldName;
        int index;

        OutputField(FieldName fieldName, int index) {
            this.fieldName = Objects.requireNonNull(fieldName, "An output field name is required.");
            this.index = index;
        }
    }

    private FieldName inputField;
    private Regex delimiter;
    private List<OutputField> outputFields;
    private boolean trimWhitespace;

    public DelimitedTextParser() {
        inputField = FieldName.of(Constants.DEFAULT_INPUT_FIELD);
        outputFields = new ArrayList<>();
        delimiter = Regex.of(DEFAULT_DELIMITER);
        trimWhitespace = Boolean.valueOf(DEFAULT_TRIM);
    }

    /**
     * @param inputField The name of the field containing the text to parse.
     */
    public DelimitedTextParser withInputField(FieldName inputField) {
        this.inputField = Objects.requireNonNull(inputField, "An input field name is required.");
        return this;
    }

    @Configurable(key = "inputField",
            label = "Input Field",
            description = "The name of the input field to parse.",
            defaultValue = Constants.DEFAULT_INPUT_FIELD)
    public DelimitedTextParser withInputField(String fieldName) {
        if (StringUtils.isNotEmpty(fieldName)) {
            withInputField(FieldName.of(fieldName));
        }
        return this;
    }

    public FieldName getInputField() {
        return inputField;
    }

    /**
     * @param delimiter A character or regular expression defining the delimiter used to split the text.
     */
    public DelimitedTextParser withDelimiter(Regex delimiter) {
        this.delimiter = Objects.requireNonNull(delimiter, "A valid delimited is required.");
        return this;
    }

    @Configurable(key = "delimiter",
            label = "Delimiter",
            description = "A regex used to split the text. Defaults to comma.",
            defaultValue = DEFAULT_DELIMITER)
    public void withDelimiter(String delimiter) {
        if (StringUtils.isNotEmpty(delimiter)) {
            withDelimiter(Regex.of(delimiter));
        }
    }

    public Regex getDelimiter() {
        return delimiter;
    }

    /**
     * @param fieldName The name of a field to create.
     * @param index     The 0-based index defining which delimited element is added to the field.
     */
    public DelimitedTextParser withOutputField(FieldName fieldName, int index) {
        outputFields.add(new OutputField(fieldName, index));
        return this;
    }

    @Configurable(key = "outputField", label = "Output Field")
    public void withOutputField(
            @Parameter(key = "fieldName", label = "Field Name", description = "The name of the output field.") String fieldName,
            @Parameter(key = "fieldIndex", label = "Column Index", description = "The index of the column containing the data.") String index) {
        if (StringUtils.isNoneBlank(fieldName, index)) {
            withOutputField(FieldName.of(fieldName), Integer.parseInt(index));
        }
    }

    public List<OutputField> getOutputFields() {
        return Collections.unmodifiableList(outputFields);
    }

    /**
     * @param trimWhitespace True, if whitespace should be trimmed from each value. Otherwise, false.
     */
    public DelimitedTextParser trimWhitespace(boolean trimWhitespace) {
        this.trimWhitespace = trimWhitespace;
        return this;
    }

    @Configurable(key = "trim",
            label = "Trim Whitespace",
            description = "Trim whitespace from each value. Defaults to true.",
            defaultValue = DEFAULT_TRIM)
    public void trimWhitespace(String trimWhitespace) {
        if (StringUtils.isNotBlank(trimWhitespace)) {
            trimWhitespace(Boolean.valueOf(trimWhitespace));
        }
    }

    public boolean isTrimWhitespace() {
        return trimWhitespace;
    }

    @Override
    public Message parse(Message input) {
        Message.Builder output = Message.builder().withFields(input);
        final Optional<FieldValue> field = input.getField(inputField);
        if (!field.isPresent()) {
            output.withError(format("Message missing expected input field '%s'", inputField.toString()));
        } else {
            doParse(field.get().toString(), output);
        }
        return output.build();
    }

    private void doParse(String valueToParse, Message.Builder output) {
        String[] columns = valueToParse.split(delimiter.toString());
        int width = columns.length;
        for (OutputField outputField : outputFields) {
            if (width > outputField.index) {
                // create a new output field
                String column = columns[outputField.index];
                if (trimWhitespace) {
                    column = column.trim();
                }
                output.addField(outputField.fieldName, StringFieldValue.of(column));

            } else {
                String err = format("Found %d column(s), index %d does not exist.", width, outputField.index);
                output.withError(err);
            }
        }
    }
}
