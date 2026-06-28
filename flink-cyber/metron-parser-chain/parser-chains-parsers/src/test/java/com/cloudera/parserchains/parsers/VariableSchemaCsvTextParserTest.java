package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.*;
import com.fasterxml.jackson.dataformat.csv.CsvReadException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class VariableSchemaCsvTextParserTest {

    private static final String DEFAULT_SETTING_FIELD_NAME = "setting";
    private static final String DEFAULT_FIELD_VALUE_PREFIX = "field:";
    private static final String DEFAULT_SETTING_VALUE = DEFAULT_FIELD_VALUE_PREFIX.concat(DEFAULT_SETTING_FIELD_NAME);
    
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
    public void testMissingFieldNameFromSetting() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new VariableSchemaCsvTextParser().withFieldNameHeader(DEFAULT_FIELD_VALUE_PREFIX));
        assertEquals("Parser setting Field Name Header does not specify a field name.", e.getMessage());
    }

    @Test
    public void testRowWithNotEnoughColumns() {
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser();
        Message input = createInputMessage(createMessageMap("1,2", "field1,field2,field3", null));
        testParseWithError(parser, input, CsvReadException.class, "Not enough column values: expected 3, found 2\n at [Source: (StringReader); line: 2, column: 3]");
    }

    @Test
    public void testRowWithNTooManyColumns() {
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser();
        Message input = createInputMessage(createMessageMap("1,2,3", "field1,field2", null));

        testParseWithError(parser, input, CsvReadException.class,
                "Too many entries: expected at most 2 (value #2 (1 chars) \"3\")\n at [Source: (StringReader); line: 2, column: 5]");
    }

    @Test
    public void testMissingInputField() {
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser();
        Message input = Message.builder().build();

        testParseWithError(parser, input, String.format("Message missing expected input field '%s'", Constants.DEFAULT_INPUT_FIELD));
    }

    private void testParseWithError(VariableSchemaCsvTextParser parser, Message input, String expectedErrorMessage) {
        testParseWithError(parser, input, IllegalStateException.class, expectedErrorMessage);
    }

    private void testParseWithError(VariableSchemaCsvTextParser parser, Message input, Class<? extends Throwable> expectedException, String expectedErrorMessage) {
        Message output = parser.parse(input);
        Optional<Throwable> error = output.getError();
        assertTrue(error.isPresent(), "should return missing input field error");
        Throwable throwable = error.get();
        assertEquals(expectedException, throwable.getClass());
        assertEquals(expectedErrorMessage, throwable.getMessage());
    }

    @Test
    public void testFailedLiteralCharacterConversion() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new VariableSchemaCsvTextParser().withDelimiter("\\u00G"));
        assertEquals("Parser setting Delimiter conversion failed due to 'Less than 4 hex digits in unicode value: '\\u00G' due to end of CharSequence'", e.getMessage());
    }

    @Test
    public void testFailedFieldValueCharacterConversion() {
        String badUnicodeChar = "\\u00G";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withDelimiter(DEFAULT_SETTING_VALUE);

        Message input = createInputMessage(createMessageMap("1,2,3", "field1,field2,field3", badUnicodeChar));

        testParseWithError(parser, input, RuntimeException.class,
                "Could not convert value \\u00G for field setting to setting Delimiter.");

    }

    @Test
    public void testOverrideFieldDelimiter() {
        String delimiterOverrideValue = " ";
        // override using literal
        testOverrideFieldDelimiter(delimiterOverrideValue, null);
        // override using field value
        testOverrideFieldDelimiter(DEFAULT_SETTING_VALUE, delimiterOverrideValue);
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

        testParser(parser, createMessageMap(csvToParse, headerToParse, delimiterInputMapValue), expectedFields);

    }
    
    @Test
    public void testMissingDelimiterField() {
        String missingFieldName = "not_set";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withDelimiter(DEFAULT_FIELD_VALUE_PREFIX.concat(missingFieldName));
        Message input = createInputMessage(createMessageMap("a,b,c", "field1,field2,field3", null));
        testParseWithError(parser, input, String.format("Message missing value for %s field '%s'", VariableSchemaCsvTextParser.DELIMITER, missingFieldName));
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
        Map<String, String> messageInputFields = new HashMap<>();
        messageInputFields.put(customInputField, csvToParse);
        messageInputFields.put(VariableSchemaCsvTextParser.DEFAULT_FIELD_NAME_HEADER , headerToParse);
        testParser(parser, messageInputFields, expectedFields);
    }

    @Test
    public void testOverrideHeaderField() {
        String headerToParse = "first,second,third,fourth";
        // override with literal
        testOverrideHeaderField(headerToParse, null);
        // override with field value
        testOverrideHeaderField(DEFAULT_FIELD_VALUE_PREFIX.concat("custom_header"), headerToParse);
    }

    private void testOverrideHeaderField(String headerParserSetting, String headerMapSetting) {
        String csvToParse = "value1,(empty),\" value, 3 \", -";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withFieldNameHeader(headerParserSetting);

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("second", "");
        expectedFields.put("third", "value, 3");
        Map<String, String> messageInputFields = new HashMap<>();
        messageInputFields.put(Constants.DEFAULT_INPUT_FIELD, csvToParse);
        if (headerMapSetting != null) {
            messageInputFields.put(headerParserSetting, headerMapSetting);
        }
        testParser(parser, messageInputFields, expectedFields);
    }

    @Test
    public void testMissingHeaderField() {
        String missingFieldName = "not_set";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withFieldNameHeader(DEFAULT_FIELD_VALUE_PREFIX.concat(missingFieldName));
        Message input = Message.builder().addField(Constants.DEFAULT_INPUT_FIELD, "a,b,c").build();
        testParseWithError(parser, input, String.format("Message missing value for %s field '%s'", VariableSchemaCsvTextParser.FIELD_NAME_HEADER, missingFieldName));
    }


    @Test
    public void testOverrideQuoteChar() {
        String quoteCharOverride = "'";
        // override with literal
        testOverrideQuoteChar(quoteCharOverride, null);

        // override with a field value
        testOverrideQuoteChar(DEFAULT_SETTING_VALUE, quoteCharOverride);
    }

    private void testOverrideQuoteChar(String quoteParserSetting, String quoteMapSetting) {
        String headerToParse = "first,second,third,fourth";
        String csvToParse = "value1, (empty),' value, 3 ',-";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withQuoteChar(quoteParserSetting);

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("second", "");
        expectedFields.put("third", "value, 3");
        testParser(parser, createMessageMap(csvToParse, headerToParse, quoteMapSetting), expectedFields);
    }

    @Test
    public void testMissingQuoteChar() {
        String missingFieldName = "not_set";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withQuoteChar(DEFAULT_FIELD_VALUE_PREFIX.concat(missingFieldName));
        Message input = createInputMessage(createMessageMap("a,b,c", "field1,field2,field3", null));
        testParseWithError(parser, input, String.format("Message missing value for %s field '%s'", VariableSchemaCsvTextParser.QUOTE_CHARACTER, missingFieldName));
    }

    @Test
    public void testOverrideEmptyFieldValue() {
        String emptyFieldOverride = "x";
        //test literal override
        testOverrideEmptyFieldValue(emptyFieldOverride, null);

        // override with a message field value
        testOverrideEmptyFieldValue(DEFAULT_SETTING_VALUE, emptyFieldOverride);
    }

    private void testOverrideEmptyFieldValue(String emptyFieldParserSetting, String emptyFieldMapSetting) {
        String headerToParse = "first,second,third,fourth";
        String csvToParse = "value1,x,\" value, 3 \",-";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withEmptyFieldValue(emptyFieldParserSetting);

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("second", "");
        expectedFields.put("third", "value, 3");
        testParser(parser, createMessageMap(csvToParse, headerToParse, emptyFieldMapSetting), expectedFields);
    }

    @Test
    public void testMissingEmptyField() {
        String missingFieldName = "not_set";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withEmptyFieldValue(DEFAULT_FIELD_VALUE_PREFIX.concat(missingFieldName));
        Message input = createInputMessage(createMessageMap("a,b,c", "field1,field2,field3", null));
        testParseWithError(parser, input, String.format("Message missing value for %s field '%s'", VariableSchemaCsvTextParser.EMPTY_FIELD_VALUE, missingFieldName));
    }

    @Test
    public void testOverrideUnsetField() {
        String unsetFieldOverride = "x";
        testOverrideUnsetField(unsetFieldOverride, null);
        testOverrideUnsetField(DEFAULT_SETTING_VALUE, unsetFieldOverride);
    }

    private void testOverrideUnsetField(String unsetFieldParserSetting, String unsetFieldMapValue) {
        String headerToParse = "first,second,third,fourth";
        String csvToParse = "value1,x,\" value, 3 \",-";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withUnsetFieldValue(unsetFieldParserSetting);

        Map<String, String> expectedFields = new HashMap<>();
        expectedFields.put("first", "value1");
        expectedFields.put("third", "value, 3");
        expectedFields.put("fourth", "-");
        testParser(parser, createMessageMap(csvToParse, headerToParse, unsetFieldMapValue), expectedFields);
    }

    @Test
    public void testMissingUnset() {
        String missingFieldName = "not_set";
        VariableSchemaCsvTextParser parser = new VariableSchemaCsvTextParser().withUnsetFieldValue(DEFAULT_FIELD_VALUE_PREFIX.concat(missingFieldName));
        Message input = createInputMessage(createMessageMap("a,b,c", "field1,field2,field3", null));
        testParseWithError(parser, input, String.format("Message missing value for %s field '%s'", VariableSchemaCsvTextParser.UNSET_FIELD_VALUE, missingFieldName));
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
        Map<String, String> messageInputFields = new HashMap<>();
        messageInputFields.put(Constants.DEFAULT_INPUT_FIELD, csvToParse);
        messageInputFields.put(VariableSchemaCsvTextParser.DEFAULT_FIELD_NAME_HEADER, headerToParse);
        testParser(parser, messageInputFields, expectedFields);
    }

    private static void testParser(VariableSchemaCsvTextParser parser, Map<String, String> messageInputFields, Map<String, String> expectedFields) {
        Message input = createInputMessage(messageInputFields);
        Message output = parser.parse(input);

        Map<String, String> fullExpectedFields = copyMessageFieldsToMap(input, new HashMap<>());
        fullExpectedFields.putAll(expectedFields);

        verifySuccessOutputMessage(output, fullExpectedFields);
    }

    private static Message createInputMessage(Map<String, String> messageInputFields) {
        Message.Builder messageBuilder = Message.builder();
        messageInputFields.forEach((name, value) -> {
            String messageFieldName = name.startsWith(DEFAULT_FIELD_VALUE_PREFIX) ? name.split(":")[1] : name;
            messageBuilder.addField(messageFieldName, value);
        });
        return messageBuilder.build();
    }

    private static Map<String, String> createMessageMap(String csvToParse, String headerToParse, String settingValue) {

        Map<String, String> messageInputFields = new HashMap<>();
        messageInputFields.put(Constants.DEFAULT_INPUT_FIELD, csvToParse);
        messageInputFields.put(VariableSchemaCsvTextParser.DEFAULT_FIELD_NAME_HEADER, headerToParse);
        if (settingValue != null) {
            messageInputFields.put("setting", settingValue);
        }

        return messageInputFields;
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
