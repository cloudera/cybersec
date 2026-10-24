package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
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
        description = "Parses delimited text like CSV or TSV using inputs provided in message metadata.")
public class VariableSchemaCsvTextParser extends AbstractTextInputParser {

    private static final String DEFAULT_DELIMITER = ",";
    private static final String DEFAULT_QUOTE_CHAR = "\"";
    private static final String DEFAULT_TRIM = "true";
    private static final String DEFAULT_EMPTY_FIELD_VALUE = "(empty)";
    private static final String DEFAULT_UNSET_FIELD_VALUE = "-";
    private static final String METADATA_VALUE_PREFIX = "metadata:";
    public static final String DEFAULT_FIELD_NAME_HEADER = "metadata:header[1]";
    public static final String INPUT_FIELD = "Input Field";
    public static final String QUOTE_CHARACTER = "Quote character";
    public static final String DELIMITER = "Delimiter";
    public static final String FIELD_NAME_HEADER = "Field Name Header";
    public static final String EMPTY_FIELD_VALUE = "Empty Field Value";
    public static final String UNSET_FIELD_VALUE = "Unset Field Value";
    public static final String DEFAULT_LINE_SEPARATOR = "\n";
    public static final String OBJECT_MAPPER_KEY_NAME = "_parser.object_mapper";
    public static final String PARSED_HEADER_KEY_NAME = "_parser.header";

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

        public abstract T getValue(Map<String, Object> metadataCache, Message.Builder output);
    }

    /**
     * Value for parser configuration provided dynamically by a field in the message.
     *
     * @param <T> The type of the field.  The value of the message field should convert from a string to this type.
     */
    private static class MetadataValueProvider<T> extends ParserSettingProvider<T> {
        /**
         * The name of the message field that will provide this value
         **/
        private final String metadataFieldName;

        /**
         * Function to convert a string value to the value required by the parser setting
         **/
        private final Function<Object, T> valueConversionFunction;

        MetadataValueProvider(String parserSettingName, String metadataFieldName, Function<Object, T> valueConversionFunction) {
            super(parserSettingName);
            this.metadataFieldName = metadataFieldName;
            this.valueConversionFunction = valueConversionFunction;
        }

        @Override
        public T getValue(Map<String, Object> metadataCache, Message.Builder output) {
            Object metadataValue = (metadataCache != null) ? metadataCache.get(metadataFieldName) : null;
            if (metadataValue != null) {
                try {
                    return valueConversionFunction.apply(metadataValue);
                } catch (Exception e) {
                    output.withError(format("Could not convert value %s for field %s to setting %s.", metadataValue, metadataFieldName, getParserSettingName()), e);
                }
            } else {
                output.withError(format("Message missing metadata value for %s field '%s'", getParserSettingName(), metadataFieldName));
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
        public T getValue(Map<String, Object> metadataCache, Message.Builder output) {
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

    private static <T> ParserSettingProvider<T> createSettingProvider(String parserSettingName, String parserSettingValue, Function<Object, T> valueConversionFunction) {
        if (parserSettingValue.startsWith(METADATA_VALUE_PREFIX)) {
            String fieldName = parserSettingValue.substring(METADATA_VALUE_PREFIX.length()).trim();
            if (!fieldName.isEmpty()) {
                return new MetadataValueProvider<>(parserSettingName, fieldName, valueConversionFunction);
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

    private static Character convertValueToChar(Object charAsObject) {
        if (charAsObject instanceof String charAsString) {
            return getFirstChar(charAsString);
        } else {
            throw new IllegalStateException(String.format("Could not convert %s to character", charAsObject));
        }
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
            description = "The header specifying the names of the fields in the CSV/TSV file.  If starts with metadata: use the value of the metadata as the header. Default value: '" + DEFAULT_FIELD_NAME_HEADER + "'",
            defaultValue = DEFAULT_FIELD_NAME_HEADER)
    public VariableSchemaCsvTextParser withFieldNameHeader(String fieldNameHeader) {
        if (StringUtils.isNotEmpty(fieldNameHeader)) {
            fieldNameHeaderProvider = createSettingProvider(FIELD_NAME_HEADER, fieldNameHeader, String.class::cast);
        }
        return this;
    }

    @Configurable(key = "quoteChar",
            label = QUOTE_CHARACTER,
            description = "A character used escape commas in text.  If set to metadata:<metadata name>, use the value of the metadata as the quote character. Default value: '" + DEFAULT_QUOTE_CHAR + "'",
            defaultValue = DEFAULT_QUOTE_CHAR)
    public VariableSchemaCsvTextParser withQuoteChar(String quoteChar) {
        if (StringUtils.isNotEmpty(quoteChar)) {
            quoteCharacterProvider = createSettingProvider(QUOTE_CHARACTER, quoteChar, VariableSchemaCsvTextParser::convertValueToChar);
        }
        return this;
    }

    @Configurable(key = "delimiter",
            label = DELIMITER,
            description = "The character used to split the text. If set to metadata:<metadata name>, use the value of the metadata as the delimiter. Default value: '" + DEFAULT_DELIMITER + "'",
            defaultValue = DEFAULT_DELIMITER)
    public VariableSchemaCsvTextParser withDelimiter(String delimiter) {
        if (StringUtils.isNotEmpty(delimiter)) {
            delimiterProvider = createSettingProvider(DELIMITER, delimiter, VariableSchemaCsvTextParser::convertValueToChar);
        }
        return this;
    }

    @Configurable(key = "emptyFieldValue",
            label = EMPTY_FIELD_VALUE,
            description = "Convert this value to empty string.  If starts with metadata: use the value of the metadata as the empty string value. Default value: '" + DEFAULT_EMPTY_FIELD_VALUE + "'",
            defaultValue = DEFAULT_EMPTY_FIELD_VALUE)
    public VariableSchemaCsvTextParser withEmptyFieldValue(String emptyFieldValue) {
        if (StringUtils.isNotEmpty(emptyFieldValue)) {
            emptyFieldValueProvider = createSettingProvider(EMPTY_FIELD_VALUE, emptyFieldValue, String.class::cast);
        }
        return this;
    }

    @Configurable(key = "unsetFieldValue",
            label = UNSET_FIELD_VALUE,
            description = "Convert this value to null.  If starts with metadata: use the value of the metadata as the unset value. Default value: '" + DEFAULT_UNSET_FIELD_VALUE + "'",
            defaultValue = DEFAULT_UNSET_FIELD_VALUE)
    public VariableSchemaCsvTextParser withUnsetFieldValue(String unsetFieldValue) {
        if (StringUtils.isNotEmpty(unsetFieldValue)) {
            unsetFieldValueProvider = createSettingProvider(UNSET_FIELD_VALUE, unsetFieldValue, String.class::cast);
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
    public Message parse(Message message) {
        return parse(message, null);
    }

    @Override
    public Message parse(Message input, Map<String, Object> metadataCache) {
        Message.Builder output = Message.builder().withFields(input);
        final Optional<FieldValue> field = input.getField(inputField);
        if (field.isEmpty()) {
            output.withError(format("Message missing expected input field '%s'", inputField.toString()));
        } else {
            doParse(unescapeJava(field.get().toString()), metadataCache, output);
        }
        return output.build();
    }

    private void doParse(String valueToParse, Map<String, Object> metadataCache, Message.Builder output) {
        try {

            ObjectReader reader = getOrCreateCsvReader(metadataCache, output);
            if (reader != null) {
                List<String> parsedHeader = getOrCreateParsedHeader(metadataCache, reader, output);
                if (parsedHeader != null) {

                    // get the empty field value
                    String emptyFieldValue = this.emptyFieldValueProvider.getValue(metadataCache, output);
                    if (emptyFieldValue == null) {
                        return;
                    }

                    // get the unset field value
                    String unsetFieldValue = this.unsetFieldValueProvider.getValue(metadataCache, output);
                    if (unsetFieldValue == null) {
                        return;
                    }
                    // parse the columns into a map
                    List<String> result = reader.readValue(valueToParse);

                    if (result.size() != parsedHeader.size()) {
                        output.withError(String.format("Message contained %s fields but header expected %s fields.", result.size(), parsedHeader.size()));
                    } else {
                        for (int i = 0; i < result.size(); i++) {
                            String fieldName = parsedHeader.get(i);
                            String fieldValue = result.get(i);

                            // trim whitespace from value if configured
                            String outputFieldValue = trimWhitespace ? fieldValue.trim() : fieldValue;

                            // if the value is not unset, add it to the output message
                            if (!unsetFieldValue.equalsIgnoreCase(outputFieldValue)) {
                                // convert empty field values to empty string
                                if (emptyFieldValue.equalsIgnoreCase(outputFieldValue)) {
                                    outputFieldValue = "";
                                }
                                output.addField(fieldName, outputFieldValue);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            output.withError(e);
        }
    }

    private ObjectReader getOrCreateCsvReader(Map<String, Object> metadataCache, Message.Builder output) {
        ObjectReader reader = metadataCache != null ? (ObjectReader)metadataCache.get(OBJECT_MAPPER_KEY_NAME) : null;

        if (reader == null) {
            reader = createCsvReader(metadataCache, output);
        }

        if (metadataCache != null) {
            metadataCache.put(OBJECT_MAPPER_KEY_NAME, reader);
        }

        return reader;
    }

    private ObjectReader createCsvReader(Map<String, Object> metadataCache, Message.Builder output) {

        // get the quote character
        Character quoteChar = this.quoteCharacterProvider.getValue(metadataCache, output);
        if (quoteChar == null) {
            return null;
        }

        // get the column separator
        Character columnSeparator = this.delimiterProvider.getValue(metadataCache, output);
        if (columnSeparator == null) {
            return null;
        }

        CsvMapper csvMapper = new CsvMapper();

        // Define a CSV schema using the first row as the header
        CsvSchema schema = CsvSchema.emptySchema()
                .withLineSeparator(DEFAULT_LINE_SEPARATOR)
                .withQuoteChar(quoteChar)
                .withColumnSeparator(columnSeparator);

        // parse the columns into a map
        return csvMapper
                .readerFor(List.class)
                .with(schema);
    }

    private List<String> getOrCreateParsedHeader(Map<String, Object> metadataCache, ObjectReader reader, Message.Builder output) throws JsonProcessingException {
        //noinspection unchecked
        List<String> parsedHeader = metadataCache != null ? (List<String>) metadataCache.get(PARSED_HEADER_KEY_NAME) : null;

        if (parsedHeader == null) {
            parsedHeader = createParsedHeader(metadataCache, reader, output);
        }
        if (metadataCache != null) {
            metadataCache.put(PARSED_HEADER_KEY_NAME, parsedHeader);
        }
        return parsedHeader;
    }

    private List<String> createParsedHeader(Map<String, Object> metadataCache, ObjectReader reader, Message.Builder output) throws JsonProcessingException {
        // get the header with the field names
        String fieldHeader = this.fieldNameHeaderProvider.getValue(metadataCache, output);
        if (fieldHeader != null) {
            return reader.readValue(fieldHeader);
        }
        return null;
    }
}
