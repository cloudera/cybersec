package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;

import static com.cloudera.parserchains.core.utils.StringUtils.getFirstChar;
import static com.cloudera.parserchains.core.utils.StringUtils.unescapeJava;
import static java.lang.String.format;

import com.cloudera.parserchains.core.Constants;
import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.FieldValue;
import com.cloudera.parserchains.core.Message;

import java.util.Optional;

/**
 * Parses delimited text like CSV.
 */
@MessageParser(
        name = "Variable Schema CSV/TSV Parser",
        description = "Parses delimited text like CSV or TSV using inputs provided in message fields or metadata.")
public class VariableSchemaCsvTextParser extends AbstractTextInputParser {

    private static final String DEFAULT_DELIMITER = ",";
    private static final String DEFAULT_QUOTE_CHAR = "\"";
    private static final String DEFAULT_TRIM = "true";
    private static final String DEFAULT_EMPTY_FIELD_VALUE = "(empty)";
    private static final String DEFAULT_UNSET_FIELD_VALUE = "-";
    private static final String FIELD_VALUE_PREFIX = "field:";
    public static final String DEFAULT_FIELD_NAME_HEADER = "field:_metadata.header[1]";
    public static final String INPUT_FIELD = "Input Field";
    public static final String QUOTE_CHARACTER = "Quote character";
    public static final String DELIMITER = "Delimiter";
    public static final String FIELD_NAME_HEADER = "Field Name Header";
    public static final String EMPTY_FIELD_VALUE = "Empty Field Value";
    public static final String UNSET_FIELD_VALUE = "Unset Field Value";
    public static final String DEFAULT_LINE_SEPARATOR = "\n";

    /**
     * The header or name of field that contains the CSV header line specifying the names of the fields.
     */
    private ParserSettingProvider<String> fieldNameHeaderProvider;

    /**
     * The name of the input field to parse.
     */
    private FieldName inputField;

    /**
     * The delimiter character or the name of the field containing the delimiter character.
     * The delimiter is either a single character or a multicharacter with escaped values.
     */
    private ParserSettingProvider<Character> delimiterProvider;

    /**
     * The quote character or the name of the field containing the quote character.
     * The quote character is either a single character or an escaped character.
     */
    private ParserSettingProvider<Character> quoteCharacterProvider;

    /**
     * The value or field name containing the string to convert to empty string.
     */
    private ParserSettingProvider<String> emptyFieldValueProvider;

    /**
     * The value or field name containing the string to convert to empty string.
     */
    private ParserSettingProvider<String> unsetFieldValueProvider;

    /**
     * If true remove whitespace from the beginning and end of field values.
     */
    private boolean trimWhitespace;

    /**
     * Gets the values for parser settings using dynamic field values or literals.
     *
     * @param <T> The type of the parser setting.
     */
    @Data
    private static abstract class ParserSettingProvider<T> {
        private final String parserSettingName;

        ParserSettingProvider(String parserSettingName) {
            this.parserSettingName = parserSettingName;
        }

        public abstract T getValue(Message input, Message.Builder output);
    }

    /**
     * Value for parser configuration provided dynamically by a field in the message.
     *
     * @param <T> The type of the field.  The value of the message field should convert from a string to this type.
     */
    private static class MessageFieldValueProvider<T> extends ParserSettingProvider<T> {
        /**
         * The name of the message field that will provide this value
         **/
        private final FieldName messageFieldName;
        /**
         * Function to convert a string value to the value required by the parser setting
         **/
        private final Function<String, T> valueConversionFunction;

        MessageFieldValueProvider(String parserSettingName, String messageFieldName, Function<String, T> valueConversionFunction) {
            super(parserSettingName);
            this.messageFieldName = FieldName.of(messageFieldName);
            this.valueConversionFunction = valueConversionFunction;
        }

        @Override
        public T getValue(Message input, Message.Builder output) {
            Optional<FieldValue> fieldValue = input.getField(messageFieldName);
            if (fieldValue.isPresent()) {
                try {
                    return valueConversionFunction.apply(fieldValue.get().get());
                } catch (Exception e) {
                    output.withError(format("Could not convert value %s for field %s to setting %s.", fieldValue.get().get(), messageFieldName, getParserSettingName()), e);
                }
            } else {
                output.withError(format("Message missing value for %s field '%s'", getParserSettingName(), messageFieldName));
            }
            return null;
        }
    }

    private static class LiteralValueProvider<T> extends ParserSettingProvider<T> {
        private final T literalValue;

        LiteralValueProvider(String parserSettingName, T literalValue) {
            super(parserSettingName);
            this.literalValue = literalValue;
        }

        @Override
        public T getValue(Message input, Message.Builder output) {
            return literalValue;
        }
    }

    public VariableSchemaCsvTextParser() {
        withInputField(Constants.DEFAULT_INPUT_FIELD);
        withFieldNameHeader(DEFAULT_FIELD_NAME_HEADER);
        withDelimiter(DEFAULT_DELIMITER);
        withQuoteChar(DEFAULT_QUOTE_CHAR);
        withEmptyFieldValue(DEFAULT_EMPTY_FIELD_VALUE);
        withUnsetFieldValue(DEFAULT_UNSET_FIELD_VALUE);
        trimWhitespace(DEFAULT_TRIM);
    }

    private static <T> ParserSettingProvider<T> createSettingProvider(String parserSettingName, String parserSettingValue, Function<String, T> valueConversionFunction) {
        if (parserSettingValue.startsWith(FIELD_VALUE_PREFIX)) {
            String fieldName = parserSettingValue.substring(FIELD_VALUE_PREFIX.length()).trim();
            if (!fieldName.isEmpty()) {
                return new MessageFieldValueProvider<>(parserSettingName, fieldName, valueConversionFunction);
            } else {
                throw new IllegalArgumentException(format("Parser setting %s does not specify a field name.", parserSettingName));
            }
        } else {
            try {
                T literalValue = valueConversionFunction.apply(parserSettingValue);
                if (literalValue != null) {
                    return new LiteralValueProvider<>(parserSettingName, literalValue);
                } else {
                    throw new IllegalArgumentException(format("Parser setting '%s' value '%s' conversion returned null", parserSettingName, parserSettingValue));
                }
            } catch (Exception e) {
                throw new IllegalArgumentException(format("Parser setting %s conversion failed due to '%s'", parserSettingName, e.getMessage()), e);
            }
        }
    }

    private static Character convertValueToChar(String charAsString) {
        return getFirstChar(charAsString);
    }

    @Configurable(key = "inputField",
            label = INPUT_FIELD,
            description = "The name of the input field to parse. Default value: '" + Constants.DEFAULT_INPUT_FIELD + "'",
            isOutputName = true,
            defaultValue = Constants.DEFAULT_INPUT_FIELD)
    public VariableSchemaCsvTextParser withInputField(String fieldName) {
        if (StringUtils.isNotEmpty(fieldName)) {
            this.inputField = FieldName.of(fieldName);
        }
        return this;
    }

    @Configurable(key = "fieldNameHeader",
            label = FIELD_NAME_HEADER,
            description = "The field name header.  If starts with field: use the value of the field as the header. Default value: '" + DEFAULT_FIELD_NAME_HEADER + "'",
            defaultValue = DEFAULT_FIELD_NAME_HEADER)
    public VariableSchemaCsvTextParser withFieldNameHeader(String fieldNameHeader) {
        if (StringUtils.isNotEmpty(fieldNameHeader)) {
            fieldNameHeaderProvider = createSettingProvider(FIELD_NAME_HEADER, fieldNameHeader, Function.identity());
        }
        return this;
    }

    @Configurable(key = "quoteChar",
            label = QUOTE_CHARACTER,
            description = "A character used escape commas in text.  If set to field:<field_name>, use the value of the field as the quote character. Default value: '" + DEFAULT_QUOTE_CHAR + "'",
            defaultValue = DEFAULT_QUOTE_CHAR)
    public VariableSchemaCsvTextParser withQuoteChar(String quoteChar) {
        if (StringUtils.isNotEmpty(quoteChar)) {
            quoteCharacterProvider = createSettingProvider(QUOTE_CHARACTER, quoteChar, VariableSchemaCsvTextParser::convertValueToChar);
        }
        return this;
    }

    @Configurable(key = "delimiter",
            label = DELIMITER,
            description = "The character used to split the text. If set to field:<field_name>, use the value of the field as the delimiter. Default value: '" + DEFAULT_DELIMITER + "'",
            defaultValue = DEFAULT_DELIMITER)
    public VariableSchemaCsvTextParser withDelimiter(String delimiter) {
        if (StringUtils.isNotEmpty(delimiter)) {
            delimiterProvider = createSettingProvider(DELIMITER, delimiter, VariableSchemaCsvTextParser::convertValueToChar);
        }
        return this;
    }

    @Configurable(key = "emptyFieldValue",
            label = EMPTY_FIELD_VALUE,
            description = "Convert this value to empty string.  If starts with field: use the value of the field as the empty string value. Default value: '" + DEFAULT_EMPTY_FIELD_VALUE + "'",
            defaultValue = DEFAULT_EMPTY_FIELD_VALUE)
    public VariableSchemaCsvTextParser withEmptyFieldValue(String emptyFieldValue) {
        if (StringUtils.isNotEmpty(emptyFieldValue)) {
            emptyFieldValueProvider = createSettingProvider(EMPTY_FIELD_VALUE, emptyFieldValue, Function.identity());
        }
        return this;
    }

    @Configurable(key = "unsetFieldValue",
            label = UNSET_FIELD_VALUE,
            description = "Convert this value to null.  If starts with field: use the value of the field as the unset value. Default value: '" + DEFAULT_UNSET_FIELD_VALUE + "'",
            defaultValue = DEFAULT_UNSET_FIELD_VALUE)
    public VariableSchemaCsvTextParser withUnsetFieldValue(String unsetFieldValue) {
        if (StringUtils.isNotEmpty(unsetFieldValue)) {
            unsetFieldValueProvider = createSettingProvider(UNSET_FIELD_VALUE, unsetFieldValue, Function.identity());
        }
        return this;
    }

    @Configurable(key = "trim",
            label = "Trim Whitespace",
            description = "Trim whitespace from each value. Default value: '" + DEFAULT_TRIM + "'",
            defaultValue = DEFAULT_TRIM)
    public VariableSchemaCsvTextParser trimWhitespace(String trimWhitespace) {
        if (StringUtils.isNotBlank(trimWhitespace)) {
            this.trimWhitespace = Boolean.parseBoolean(trimWhitespace);
        }
        return this;
    }

    @Override
    public Message parse(Message input) {
        Message.Builder output = Message.builder().withFields(input);
        final Optional<FieldValue> field = input.getField(inputField);
        if (field.isEmpty()) {
            output.withError(format("Message missing expected input field '%s'", inputField.toString()));
        } else {
            doParse(unescapeJava(field.get().toString()), input, output);
        }
        return output.build();
    }

    private void doParse(String valueToParse, Message input, Message.Builder output) {
        try {
            // get the header with the field names
            String fieldHeader = this.fieldNameHeaderProvider.getValue(input, output);
            if (fieldHeader == null) {
                return;
            }

            // get the quote character
            Character quoteChar = this.quoteCharacterProvider.getValue(input, output);
            if (quoteChar == null) {
                return;
            }

            // get the column separator
            Character columnSeparator = this.delimiterProvider.getValue(input, output);
            if (columnSeparator == null) {
                return;
            }

            // get the empty field value
            String emptyFieldValue = this.emptyFieldValueProvider.getValue(input, output);
            if (emptyFieldValue == null) {
                return;
            }

            // get the unset field value
            String unsetFieldValue = this.unsetFieldValueProvider.getValue(input, output);
            if (unsetFieldValue == null) {
                return;
            }

            CsvMapper csvMapper = new CsvMapper()
                    // fail if not enough columns
                    .enable(CsvParser.Feature.FAIL_ON_MISSING_COLUMNS)
                    // fail if there are too many columns
                    .disable(CsvParser.Feature.IGNORE_TRAILING_UNMAPPABLE);

            // Define a CSV schema using the first row as the header
            CsvSchema schema = CsvSchema.emptySchema()
                    .withHeader()
                    .withLineSeparator(DEFAULT_LINE_SEPARATOR)
                    .withQuoteChar(quoteChar)
                    .withColumnSeparator(columnSeparator);

            // construct the header and line to parse
            String valueWithHeader = fieldHeader.concat(DEFAULT_LINE_SEPARATOR).concat(valueToParse);

            // parse the columns into a map
            Map<String, String> result = csvMapper
                    .readerFor(Map.class)
                    .with(schema)
                    .readValue(valueWithHeader);

            // remove unset fields
            result.values().removeIf(unsetFieldValue::equalsIgnoreCase);

            // add fields to output message
            //      converting empty fields to empty string
            //      trimming non-empty values
            // omit fields that are unset
            result.forEach((fieldName, fieldValue) -> {
                // trim extra space from fields, if configured
                String outputFieldValue = trimWhitespace ? fieldValue.trim() : fieldValue;
                // convert empty field values to empty string
                if (emptyFieldValue.equalsIgnoreCase(outputFieldValue)) {
                    outputFieldValue = "";
                }
                // if the value is not unset, add it to the output message
                if (!unsetFieldValue.equalsIgnoreCase(outputFieldValue)) {
                    output.addField(fieldName, outputFieldValue);
                }
            });
        } catch (JsonProcessingException e) {
            output.withError(e);
        }
    }

}
