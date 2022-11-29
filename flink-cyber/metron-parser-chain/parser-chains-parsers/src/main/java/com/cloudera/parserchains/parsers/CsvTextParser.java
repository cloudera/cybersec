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
import com.cloudera.parserchains.core.StringFieldValue;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.cloudera.parserchains.core.catalog.Parameter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static com.cloudera.parserchains.core.utils.StringUtils.getFirstChar;
import static com.cloudera.parserchains.core.utils.StringUtils.unescapeJava;
import static java.lang.String.format;

/**
 * Parses delimited text like CSV.
 */
@MessageParser(
        name = "CSV/TSV Parser",
        description = "Parses delimited text like CSV or TSV.")
public class CsvTextParser implements Parser {

    private static final String DEFAULT_DELIMITER = ",";
    private static final String DEFAULT_QUOTE_CHAR = "\"";
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

    private final CsvMapper mapper;
    private ObjectReader reader;
    private CsvSchema schema;

    private final List<OutputField> outputFields;
    private FieldName inputField;
    private boolean trimWhitespace;

    public CsvTextParser() {
        inputField = FieldName.of(Constants.DEFAULT_INPUT_FIELD);
        outputFields = new ArrayList<>();
        trimWhitespace = Boolean.parseBoolean(DEFAULT_TRIM);
        mapper = new CsvMapper();
        updateSchema(() -> mapper
                .schemaFor(new TypeReference<List<String>>() {
                })
                .withoutHeader()
                .withLineSeparator("\n")
                .withColumnSeparator(getFirstChar(DEFAULT_DELIMITER))
                .withQuoteChar(getFirstChar(DEFAULT_QUOTE_CHAR)));
    }

    /**
     * @param inputField The name of the field containing the text to parse.
     */
    public CsvTextParser withInputField(FieldName inputField) {
        this.inputField = Objects.requireNonNull(inputField, "An input field name is required.");
        return this;
    }

    @Configurable(key = "inputField",
            label = "Input Field",
            description = "The name of the input field to parse.",
            defaultValue = Constants.DEFAULT_INPUT_FIELD)
    public CsvTextParser withInputField(String fieldName) {
        if (StringUtils.isNotEmpty(fieldName)) {
            withInputField(FieldName.of(fieldName));
        }
        return this;
    }

    public FieldName getInputField() {
        return inputField;
    }

    /**
     * @param quoteChar A character replacing the quote character used for escaping when parsing CSV.
     */
    public CsvTextParser withQuoteChar(char quoteChar) {
        updateSchema(() -> this.schema.withQuoteChar(quoteChar));
        return this;
    }

    @Configurable(key = "quoteChar",
            label = "Quote character",
            description = "A character used escape commas in text. Defaults to double-quote.",
            defaultValue = DEFAULT_QUOTE_CHAR)
    public void withQuoteChar(String quoteChar) {
        if (StringUtils.isNotEmpty(quoteChar)) {
            withQuoteChar(getFirstChar(quoteChar));
        }
    }

    public char getQuoteChar() {
        return (char) this.schema.getQuoteChar();
    }

    /**
     * @param delimiter A character defining the delimiter used to split the text.
     */
    public CsvTextParser withDelimiter(char delimiter) {
        updateSchema(() -> this.schema.withColumnSeparator(delimiter));
        return this;
    }

    @Configurable(key = "delimiter",
            label = "Delimiter",
            description = "A character used to split the text. Defaults to comma.",
            defaultValue = DEFAULT_DELIMITER)
    public void withDelimiter(String delimiter) {
        if (StringUtils.isNotEmpty(delimiter)) {
            withDelimiter(getFirstChar(delimiter));
        }
    }

    public char getDelimiter() {
        return this.schema.getColumnSeparator();
    }

    /**
     * @param fieldName The name of a field to create.
     * @param index     The 0-based index defining which delimited element is added to the field.
     */
    public CsvTextParser withOutputField(FieldName fieldName, int index) {
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
    public CsvTextParser trimWhitespace(boolean trimWhitespace) {
        this.trimWhitespace = trimWhitespace;
        return this;
    }

    @Configurable(key = "trim",
            label = "Trim Whitespace",
            description = "Trim whitespace from each value. Defaults to true.",
            defaultValue = DEFAULT_TRIM)
    public void trimWhitespace(String trimWhitespace) {
        if (StringUtils.isNotBlank(trimWhitespace)) {
            trimWhitespace(Boolean.parseBoolean(trimWhitespace));
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
            doParse(unescapeJava(field.get().toString()), output);
        }
        return output.build();
    }

    private void doParse(String valueToParse, Message.Builder output) {
        try {
            final List<String> valueList = reader
                    .readValue(valueToParse);

            for (OutputField outputField : outputFields) {
                final int index = outputField.index;
                if (valueList.size() > index) {
                    String value = valueList.get(index);
                    if (trimWhitespace) {
                        value = value.trim();
                    }
                    output.addField(outputField.fieldName, StringFieldValue.of(value));
                } else {
                    final String err = format("Column index %d is out of bounds for %s", index, outputField.fieldName);
                    output.withError(err);
                }
            }
        } catch (JsonProcessingException e) {
            output.withError(e);
        }
    }

    private void updateSchema(Supplier<CsvSchema> csvSchemaConsumer) {
        this.schema = csvSchemaConsumer.get();
        this.reader = mapper
                .readerFor(new TypeReference<List<String>>() {
                })
                .with(schema);
    }
}
