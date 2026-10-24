package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.cloudera.parserchains.parsers.VariableSchemaCsvTextParser.DEFAULT_FIELD_NAME_HEADER;
import static com.cloudera.parserchains.parsers.VariableSchemaCsvTextParser.OBJECT_MAPPER_KEY_NAME;
import static com.cloudera.parserchains.parsers.VariableSchemaCsvTextParser.PARSED_HEADER_KEY_NAME;
import static org.junit.jupiter.api.Assertions.*;

public class VariableSchemaCsvTextParserTest {

    public static final String BAD_UNICODE_CHAR = "\\u00G";
    private static final String DEFAULT_SETTING_FIELD_NAME = "setting";
    private static final String DEFAULT_METADATA_VALUE_PREFIX = "metadata:";
    private static final String DEFAULT_METADATA_PARSER_SETTING_VALUE = DEFAULT_METADATA_VALUE_PREFIX.concat(DEFAULT_SETTING_FIELD_NAME);
    
    @Test
    public void testDefaultParser() {

        String headerToParse = "first,second,third,fourth";
        String csvToParse = "value1,(empty),\" value, 3 \", -";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser();


        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("second", "");
        expectedFields.put("third", "value, 3");
        testParser(parser, csvToParse, headerToParse, expectedFields);

        // empty string settings have no effect
        parser.withUnsetFieldValue("").withDelimiter("").withUnsetFieldValue("").withEmptyFieldValue("").
                withQuoteChar("").withInputField("").withFieldNameHeader("").trimWhitespace("");
        testParser(parser, csvToParse, headerToParse, expectedFields);
    }

    @Test
    public void testParserWithNoMetadataAccess() {
        String headerToParse = "first,second,third,fourth";
        String csvToParse = "value1,(empty),\" value, 3 \", -";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withFieldNameHeader(headerToParse);

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put(Constants.DEFAULT_INPUT_FIELD, csvToParse);
        expectedFields.put("first", "value1");
        expectedFields.put("second", "");
        expectedFields.put("third", "value, 3");
        Message output = parser.parse(createInputMessage(csvToParse));
        verifySuccessOutputMessage(output, expectedFields);
    }

    @Test
    public void testParserAccessingNullMetadata() {
        String csvToParse = "value1,(empty),\" value, 3 \", -";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser();

        Message output = parser.parse(createInputMessage(csvToParse));
        verifyParserError(output, IllegalStateException.class, "Message missing metadata value for Field Name Header field 'header[1]'");
    }

    @Test
    public void testMissingFieldNameFromSetting() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new VariableSchemaCsvTextParser().withFieldNameHeader(DEFAULT_METADATA_VALUE_PREFIX));
        assertEquals("Parser setting Field Name Header does not specify a field name.", e.getMessage());
    }

    @Test
    public void testRowWithNotEnoughColumns() {
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser();
        Map<String, Object> metadata = createMetadata("field1,field2,field3", null);

        testParseWithError(parser, metadata, createInputMessage("1,2") , "Message contained 2 fields but header expected 3 fields.");
     }

    @Test
    public void testRowWithNTooManyColumns() {
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser();

        testParseWithError(parser, createMetadata("field1,field2", null),
                createInputMessage("1,2,3"),
                "Message contained 3 fields but header expected 2 fields.");
    }

    @Test
    public void testMissingInputField() {
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser();
        Message input = Message.builder().build();

        testParseWithError(parser, null, input, String.format("Message missing expected input field '%s'", Constants.DEFAULT_INPUT_FIELD));
    }

    private void testParseWithError(VariableSchemaCsvTextParser parser, Map<String, Object> metadata, Message input, String expectedErrorMessage) {
        testParseWithError(parser, metadata, input, IllegalStateException.class, expectedErrorMessage);
    }

    private void testParseWithError(VariableSchemaCsvTextParser parser, Map<String, Object> metadata, Message input, Class<? extends Throwable> expectedException, String expectedErrorMessage) {
        Message output = parser.parse(input, metadata);
        verifyParserError(output, expectedException, expectedErrorMessage);
    }

    private void verifyParserError(Message output, Class<? extends Throwable> expectedException, String expectedErrorMessage) {
        Optional<Throwable> error = output.getError();
        assertTrue(error.isPresent(), "should return missing input field error");
        Throwable throwable = error.get();
        assertEquals(expectedException, throwable.getClass());
        assertEquals(expectedErrorMessage, throwable.getMessage());
    }

    @Test
    public void testFailedLiteralCharacterConversion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new VariableSchemaCsvTextParser().withDelimiter(BAD_UNICODE_CHAR));
        assertEquals("Parser setting Delimiter conversion failed due to 'Less than 4 hex digits in unicode value: '\\u00G' due to end of CharSequence'", e.getMessage());
    }

    @Test
    public void testFailedFieldValueCharacterConversion() {
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withDelimiter(DEFAULT_METADATA_PARSER_SETTING_VALUE);

        testParseWithError(parser, createMetadata("field1,field2,field3", BAD_UNICODE_CHAR),
                createInputMessage("field1,field2,field3"), RuntimeException.class,
                "Could not convert value \\u00G for field setting to setting Delimiter.");

    }

    @Test
    public void testOverrideFieldDelimiter() {
        String delimiterOverrideValue = " ";
        // override using literal
        testOverrideFieldDelimiter(delimiterOverrideValue, null);
        // override using field value
        testOverrideFieldDelimiter(DEFAULT_METADATA_PARSER_SETTING_VALUE, delimiterOverrideValue);
    }

    private void testOverrideFieldDelimiter(String delimiterParserSetting, String delimiterInputMapValue) {
        String headerToParse = "first second third fourth";
        String csvToParse = "value1 (empty) \" value, 3 \" -";
        // test with a fixed value
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withDelimiter(delimiterParserSetting);

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("second", "");
        expectedFields.put("third", "value, 3");

        testParser(parser, createMetadata(headerToParse, delimiterInputMapValue),  createInputMessage(csvToParse), expectedFields);

    }
    
    @Test
    public void testMissingDelimiterField() {
        String missingFieldName = "not_set";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withDelimiter(DEFAULT_METADATA_VALUE_PREFIX.concat(missingFieldName));

        testParseWithError(parser, createMetadata("field1,field2,field3", null), createInputMessage("a,b,c"), String.format("Message missing metadata value for %s field '%s'", VariableSchemaCsvTextParser.DELIMITER, missingFieldName));
    }

    @Test
    public void testOverrideInputField() {
        String headerToParse = "first,second,third,fourth";
        String csvToParse = "value1,(empty),\" value, 3 \", -";
        String customInputField = "custom_input";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withInputField(customInputField);

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("second", "");
        expectedFields.put("third", "value, 3");
        testParser(parser, createMetadata(headerToParse, null), createInputMessage(customInputField, csvToParse), expectedFields);
    }

    @Test
    public void testOverrideHeaderField() {
        String headerToParse = "first,second,third,fourth";
        // override with literal
        testOverrideHeaderField(headerToParse, null);
        // override with field value
        testOverrideHeaderField(DEFAULT_METADATA_VALUE_PREFIX.concat("custom_header"), headerToParse);
    }

    private void testOverrideHeaderField(String headerParserSetting, String headerMapSetting) {
        String csvToParse = "value1,(empty),\" value, 3 \", -";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withFieldNameHeader(headerParserSetting);

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("second", "");
        expectedFields.put("third", "value, 3");
        Map<String, Object> metadata = new HashMap<>();
        if (headerMapSetting != null && headerParserSetting.startsWith(DEFAULT_METADATA_VALUE_PREFIX)) {
            metadata.put(headerParserSetting.split(":")[1], headerMapSetting);
        }
        testParser(parser, metadata, createInputMessage(csvToParse), expectedFields);
    }

    @Test
    public void testMissingHeaderField() {
        String missingFieldName = "not_set";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withFieldNameHeader(DEFAULT_METADATA_VALUE_PREFIX.concat(missingFieldName));
        testParseWithError(parser, new HashMap<>(), createInputMessage("a,b,c"), String.format("Message missing metadata value for %s field '%s'", VariableSchemaCsvTextParser.FIELD_NAME_HEADER, missingFieldName));
    }


    @Test
    public void testOverrideQuoteChar() {
        String quoteCharOverride = "'";
        // override with literal
        testOverrideQuoteChar(quoteCharOverride, null);

        // override with a field value
        testOverrideQuoteChar(DEFAULT_METADATA_PARSER_SETTING_VALUE, quoteCharOverride);
    }

    private void testOverrideQuoteChar(String quoteParserSetting, String quoteMapSetting) {
        String headerToParse = "first,second,third,fourth";
        String csvToParse = "value1, (empty),' value, 3 ',-";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withQuoteChar(quoteParserSetting);

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("second", "");
        expectedFields.put("third", "value, 3");
        testParser(parser, createMetadata(headerToParse, quoteMapSetting), createInputMessage(csvToParse), expectedFields);
    }

    @Test
    public void testMissingQuoteChar() {
        String missingFieldName = "not_set";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withQuoteChar(DEFAULT_METADATA_VALUE_PREFIX.concat(missingFieldName));
        testParseWithError(parser, createMetadata("field1,field2,field3", null), createInputMessage("a,b,c"), String.format("Message missing metadata value for %s field '%s'", VariableSchemaCsvTextParser.QUOTE_CHARACTER, missingFieldName));
    }

    @Test
    public void testOverrideEmptyFieldValue() {
        String emptyFieldOverride = "x";
        //test literal override
        testOverrideEmptyFieldValue(emptyFieldOverride, null);

        // override with a message field value
        testOverrideEmptyFieldValue(DEFAULT_METADATA_PARSER_SETTING_VALUE, emptyFieldOverride);
    }

    private void testOverrideEmptyFieldValue(String emptyFieldParserSetting, String emptyFieldMapSetting) {
        String headerToParse = "first,second,third,fourth";
        String csvToParse = "value1,x,\" value, 3 \",-";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withEmptyFieldValue(emptyFieldParserSetting);

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("second", "");
        expectedFields.put("third", "value, 3");
        testParser(parser, createMetadata(headerToParse, emptyFieldMapSetting), createInputMessage(csvToParse), expectedFields);
    }

    @Test
    public void testMissingEmptyField() {
        String missingFieldName = "not_set";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withEmptyFieldValue(DEFAULT_METADATA_VALUE_PREFIX.concat(missingFieldName));
        testParseWithError(parser, createMetadata("field1,field2,field3", null), createInputMessage("a,b,c"), String.format("Message missing metadata value for %s field '%s'", VariableSchemaCsvTextParser.EMPTY_FIELD_VALUE, missingFieldName));
    }

    @Test
    public void testOverrideUnsetField() {
        String unsetFieldOverride = "x";
        testOverrideUnsetField(unsetFieldOverride, null);
        testOverrideUnsetField(DEFAULT_METADATA_PARSER_SETTING_VALUE, unsetFieldOverride);
    }

    private void testOverrideUnsetField(String unsetFieldParserSetting, String unsetFieldMapValue) {
        String headerToParse = "first,second,third,fourth";
        String csvToParse = "value1,x,\" value, 3 \",-";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withUnsetFieldValue(unsetFieldParserSetting);

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("third", "value, 3");
        expectedFields.put("fourth", "-");
        testParser(parser, createMetadata(headerToParse, unsetFieldMapValue), createInputMessage(csvToParse), expectedFields);
    }

    @Test
    public void testMissingUnset() {
        String missingFieldName = "not_set";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withUnsetFieldValue(DEFAULT_METADATA_VALUE_PREFIX.concat(missingFieldName));
        testParseWithError(parser, createMetadata("field1,field2,field3", null), createInputMessage("a,b,c"), String.format("Message missing metadata value for %s field '%s'", VariableSchemaCsvTextParser.UNSET_FIELD_VALUE, missingFieldName));
    }

    @Test
    public void testOverrideTrimWhitespace() {
        String headerToParse = "first,second,third,fourth";
        String csvToParse = "value1, value2,\" value, 3 \",-";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().trimWhitespace("false");

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("second", " value2");
        expectedFields.put("third", " value, 3 ");
        testParser(parser, csvToParse, headerToParse, expectedFields);
    }

    private static void testParser(VariableSchemaCsvTextParser parser,  String csvToParse, String headerToParse, Map<String, String> expectedFields) {
        Map<String, Object> metadata = createMetadata(headerToParse, null);
        Message input = createInputMessage(csvToParse);

        // verify parser without cached metadata
        testParser(parser, metadata, input, expectedFields);

        // check the metadata cached entries are added
        assertTrue(metadata.containsKey(OBJECT_MAPPER_KEY_NAME));
        assertTrue(metadata.containsKey(PARSED_HEADER_KEY_NAME));

        // verify that the same message produces the same results with cache
        testParser(parser, metadata, input, expectedFields);
    }

    private static void testParser(VariableSchemaCsvTextParser parser, Map<String, Object> metadata, Message input, Map<String, String> expectedFields) {
        Message output = parser.parse(input, metadata);

        Map<String, String> fullExpectedFields = copyMessageFieldsToMap(input, new HashMap<>());
        fullExpectedFields.putAll(expectedFields);

        verifySuccessOutputMessage(output, fullExpectedFields);
    }

    private static Message createInputMessage(String stringToParse) {
        return createInputMessage(Constants.DEFAULT_INPUT_FIELD, stringToParse);
    }

    private static Message createInputMessage(String inputMessageFieldName, String stringToParse) {
        return Message.builder().addField(inputMessageFieldName, stringToParse).build();
    }

    private static Map<String, Object> createMetadata(String headerToParse, String settingValue) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(DEFAULT_FIELD_NAME_HEADER.split(":")[1], headerToParse);
        if (settingValue != null) {
            metadata.put(DEFAULT_SETTING_FIELD_NAME, settingValue);
        }
        return metadata;
    }

    private static void verifySuccessOutputMessage(Message output, Map<String, String> expectedFields) {
        if (output.getError().isPresent()) {
            fail(String.format("Expected no parsing errors. Got error message: %s", output.getError().get()));
        }
        Map<String, String> actualFields = copyMessageFieldsToMap(output, new HashMap<>());

        assertEquals(expectedFields, actualFields, "Message fields do not match expected values.");
    }

    private static Map<String, String> copyMessageFieldsToMap(Message message, Map<String, String> fields) {
        for (Map.Entry<FieldName, FieldValue> actualField : message.getFields().entrySet()) {
            fields.put(actualField.getKey().get(), actualField.getValue().get());
        }
        return fields;
    }
}
