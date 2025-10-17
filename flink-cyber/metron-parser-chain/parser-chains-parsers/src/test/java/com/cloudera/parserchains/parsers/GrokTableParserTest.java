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
import com.cloudera.parserchains.core.Message;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;


public class GrokTableParserTest {
    private static final String CISCO_LOG_MESSAGE = "<164>Aug 05 2016 01:01:34: %ASA-4-106023: Deny tcp src Inside:10.30.9.121/54580 dst Outside:192.168.135.51/42028 by access-group \"Inside_access_in\" [0x962df600, 0x0]";
    private static final String UNMAPPED_CISCO_LOG_MESSAGE = "<164>Aug 05 2016 01:01:34: %ASA-1-123456: Deny tcp src Inside:10.30.9.121/54580 dst Outside:192.168.135.51/42028 by access-group \"Inside_access_in\" [0x962df600, 0x0]";
    private static final String CISCO_CONNECTION_MESSAGE = "<174>Jan  5 14:52:35 10.22.8.212 %ASA-6-302015: Built inbound UDP connection 76245506 for outside:10.22.8.110/49886 (10.22.8.110/49886) to inside:192.111.72.8/8612 (192.111.72.8/8612) (user.name)";
    private static final String CISCO_MESSAGE_GROK_PATTERNS_PATH = "/grok/cisco_asa";
    private static final String INPUT_FIELD_NAME = "input";
    private static final String CISCO_INITIAL_GROK_EXPRESSION = "%{CISCO_TAGGED_SYSLOG}";
    private static final String CISCO_KEY_FIELD_NAME = "cisco_tag";
    private static final String CISCO_MESSAGE_FIELD_NAME = "message";

    private GrokTableParser grokTableParser;

    @BeforeEach
    void beforeEach() {
        grokTableParser = new GrokTableParser();
    }

    @Test
    void testValidConfigurationWithDefaultInput() {
        testSuccessfulMessages(Constants.DEFAULT_INPUT_FIELD);
    }

    @Test
    void testValidConfigurationWithInputField() {
        testSuccessfulMessages(INPUT_FIELD_NAME);
    }

    @Test
    void testEmptyMessage() {
        String messageWithEmptyText = "%ASA-4-106023:      ";

        String patternPath = getFileFromResource(CISCO_MESSAGE_GROK_PATTERNS_PATH).getAbsolutePath();

        grokTableParser
                .inputField(FieldName.of(INPUT_FIELD_NAME))
                .grokPatternPath(patternPath)
                .initialGrokExpression("%{CISCOTAG:cisco_tag}:%{GREEDYDATA:message}")
                .keyFieldName(CISCO_KEY_FIELD_NAME)
                .messageFieldName(CISCO_MESSAGE_FIELD_NAME);

        Message messageMissingInput = Message.builder().
                addField(INPUT_FIELD_NAME, messageWithEmptyText)
                .build();
        Message output = grokTableParser.parse(messageMissingInput);

        checkErrorMessage(output, String.format(GrokTableParser.MESSAGE_FIELD_NULL_AFTER_GROK_CAPTURE_ERROR_FORMAT, CISCO_MESSAGE_FIELD_NAME));
    }

    private void testSuccessfulMessages(String inputFieldName) {
        String patternPath = getFileFromResource(CISCO_MESSAGE_GROK_PATTERNS_PATH).getAbsolutePath();

        grokTableParser
                .grokPatternPath(patternPath)
                .initialGrokExpression(CISCO_INITIAL_GROK_EXPRESSION)
                .keyFieldName(CISCO_KEY_FIELD_NAME)
                .messageFieldName(CISCO_MESSAGE_FIELD_NAME);

        if (!Constants.DEFAULT_INPUT_FIELD.equals(inputFieldName)) {
            grokTableParser.inputField(inputFieldName);
        }

        Message expectedParsedDenyMessage = Message.builder()
                .addField("timestamp", "Aug 05 2016 01:01:34")
                .addField(inputFieldName, CISCO_LOG_MESSAGE)
                .addField("action", "Deny")
                .addField("src_ip", "10.30.9.121")
                .addField("hashcode1", "0x962df600")
                .addField("hashcode2", "0x0")
                .addField("dst_interface", "Outside")
                .addField("protocol", "tcp")
                .addField("src_interface", "Inside")
                .addField("policy_id", "Inside_access_in")
                .addField("dst_port", "42028")
                .addField("syslog_pri", "164")
                .addField("message", ": Deny tcp src Inside:10.30.9.121/54580 dst Outside:192.168.135.51/42028 by access-group \"Inside_access_in\" [0x962df600, 0x0]")
                .addField("dst_ip", "192.168.135.51")
                .addField("src_port", "54580")
                .addField("cisco_tag", "ASA-4-106023")
                .build();

        testMessageParse(inputFieldName, CISCO_LOG_MESSAGE, expectedParsedDenyMessage);

        Message expectedParsedConnectionMessage = Message.builder()
                .addField("timestamp", "Jan  5 14:52:35")
                .addField(inputFieldName, CISCO_CONNECTION_MESSAGE)
                .addField("action", "Built")
                .addField("src_ip", "10.22.8.110")
                .addField("dst_interface", "inside")
                .addField("dst_mapped_ip", "192.111.72.8")
                .addField("protocol", "UDP").addField("src_interface", "outside")
                .addField("dst_port", "8612")
                .addField("syslog_pri", "174")
                .addField("direction", "inbound")
                .addField("syslog_host", "10.22.8.212")
                .addField("message", ": Built inbound UDP connection 76245506 for outside:10.22.8.110/49886 (10.22.8.110/49886) to inside:192.111.72.8/8612 (192.111.72.8/8612) (user.name)")
                .addField("src_mapped_ip", "10.22.8.110")
                .addField("connection_id", "76245506")
                .addField("dst_ip", "192.111.72.8")
                .addField("src_mapped_port", "49886")
                .addField("dst_mapped_port", "8612")
                .addField("reason", "")
                .addField("src_port", "49886")
                .addField("cisco_tag", "ASA-6-302015")
                .build();
        testMessageParse(inputFieldName, CISCO_CONNECTION_MESSAGE, expectedParsedConnectionMessage);
    }

    private void testMessageParse(String inputFieldName, String messageToParse, Message expected) {
        Message input = Message.builder()
                .addField(inputFieldName, messageToParse)
                .build();
        Message output = grokTableParser.parse(input);

        assertThat("Parsed message fields do not match expected values.",
                output, is(expected));
        assertFalse(output.getError().isPresent(), "Did not expect errors in parsing");
    }

    @Test
    public void testBadGrokExpressionInPath() {
        String patternPath = getFileFromResource("/grok/bad_grok_pattern").getAbsolutePath();

        grokTableParser
                .grokPatternPath(patternPath)
                .initialGrokExpression("%{INT:message_id} : %{GREEDYDATA:message}")
                .keyFieldName("message_id")
                .messageFieldName("message");

        Message output = grokTableParser.parse(Message.builder().addField(grokTableParser.getInputField().get(), "1234 : this is a test").build());
        checkErrorMessage(output, String.format(GrokTableParser.FAILED_MESSAGE_GROK_COMPILATION_ERROR_FORMAT, "1234", "Illegal repetition near index 0\n%{INT\n^"));
    }

    @Test
    public void testBadInitialGrokExpression() {
        String patternPath = getFileFromResource(CISCO_MESSAGE_GROK_PATTERNS_PATH).getAbsolutePath();

        grokTableParser
                .grokPatternPath(patternPath)
                .keyFieldName(CISCO_KEY_FIELD_NAME)
                .messageFieldName(CISCO_MESSAGE_FIELD_NAME);

        try {
            grokTableParser.initialGrokExpression("%{UNDEFINED_GROK_PATTERN}");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            Message output = grokTableParser.parse(Message.builder().addField(grokTableParser.getInputField().get(), "1234 : this is a test").build());
            checkErrorMessage(output, String.format(GrokTableParser.MISSING_REQUIRED_CONFIGURATION_FIELD_ERROR_FORMAT, GrokTableParser.INITIAL_GROK_EXPRESSION_CONFIGURATION));
        }

    }

    @Test
    public void testNoMatchingExpressionForMessageKeyReturnsInitialGrokFields() {
        String patternPath = getFileFromResource(CISCO_MESSAGE_GROK_PATTERNS_PATH).getAbsolutePath();

        grokTableParser
                .grokPatternPath(patternPath)
                .initialGrokExpression(CISCO_INITIAL_GROK_EXPRESSION)
                .keyFieldName(CISCO_KEY_FIELD_NAME)
                .messageFieldName(CISCO_MESSAGE_FIELD_NAME);


        Message expectedMessage = Message.builder()
                .addField("timestamp", "Aug 05 2016 01:01:34")
                .addField(grokTableParser.getInputField().get(), UNMAPPED_CISCO_LOG_MESSAGE)
                .addField("syslog_pri", "164")
                .addField("message", ": Deny tcp src Inside:10.30.9.121/54580 dst Outside:192.168.135.51/42028 by access-group \"Inside_access_in\" [0x962df600, 0x0]")
                .addField("cisco_tag", "ASA-1-123456")
                .build();

        testMessageParse(grokTableParser.getInputField().get(), UNMAPPED_CISCO_LOG_MESSAGE, expectedMessage);
    }

    @Test
    public void testMissingKeyFieldValue() {
        String patternPath = getFileFromResource(CISCO_MESSAGE_GROK_PATTERNS_PATH).getAbsolutePath();
        String fieldNotInGrok = "field_not_in_grok";
        grokTableParser
                .inputField(FieldName.of(INPUT_FIELD_NAME))
                .grokPatternPath(patternPath)
                .initialGrokExpression(CISCO_INITIAL_GROK_EXPRESSION)
                .keyFieldName(fieldNotInGrok)
                .messageFieldName(CISCO_MESSAGE_FIELD_NAME);

        Message messageMissingInput = Message.builder().
                addField(INPUT_FIELD_NAME, CISCO_LOG_MESSAGE)
                .build();
        Message output = grokTableParser.parse(messageMissingInput);

        checkErrorMessage(output, String.format(GrokTableParser.MESSAGE_KEY_FIELD_NULL_AFTER_GROK_CAPTURE_ERROR_FORMAT, fieldNotInGrok));
    }

    @Test
    public void testMissingMessageFieldValue() {
        String patternPath = getFileFromResource(CISCO_MESSAGE_GROK_PATTERNS_PATH).getAbsolutePath();
        String fieldNotInGrok = "field_not_in_grok";
        grokTableParser
                .inputField(FieldName.of(INPUT_FIELD_NAME))
                .grokPatternPath(patternPath)
                .initialGrokExpression(CISCO_INITIAL_GROK_EXPRESSION)
                .keyFieldName(CISCO_KEY_FIELD_NAME)
                .messageFieldName(fieldNotInGrok);

        Message messageMissingInput = Message.builder().
                addField(INPUT_FIELD_NAME, CISCO_LOG_MESSAGE)
                .build();
        Message output = grokTableParser.parse(messageMissingInput);

        checkErrorMessage(output, String.format(GrokTableParser.MESSAGE_FIELD_NULL_AFTER_GROK_CAPTURE_ERROR_FORMAT, fieldNotInGrok));
    }


    @Test
    public void testInputMessageMissingFromMessageToParse() {
        String patternPath = getFileFromResource(CISCO_MESSAGE_GROK_PATTERNS_PATH).getAbsolutePath();

        grokTableParser
                .inputField(FieldName.of(INPUT_FIELD_NAME))
                .grokPatternPath(patternPath)
                .initialGrokExpression(CISCO_INITIAL_GROK_EXPRESSION)
                .keyFieldName(CISCO_KEY_FIELD_NAME)
                .messageFieldName(CISCO_MESSAGE_FIELD_NAME);

        Message messageMissingInput = Message.builder().
                addField("wrong_input_field_name", "Message won' be parsed")
                .build();
        Message output = grokTableParser.parse(messageMissingInput);

        checkErrorMessage(output, String.format(GrokTableParser.MESSAGE_TO_PARSE_EXPECTED_INPUT_FIELD_ERROR_FORMAT, INPUT_FIELD_NAME));
    }

    @Test
    public void testMissingKeyFieldName() {
        testParserConfigError(null, CISCO_MESSAGE_FIELD_NAME, CISCO_INITIAL_GROK_EXPRESSION, CISCO_MESSAGE_GROK_PATTERNS_PATH);
    }

    @Test
    public void testMissingMessageFieldName() {
        testParserConfigError(CISCO_KEY_FIELD_NAME, null, CISCO_INITIAL_GROK_EXPRESSION, CISCO_MESSAGE_GROK_PATTERNS_PATH);
    }

    @Test
    public void testMissingInitialGrokExpression() {
        testParserConfigError(CISCO_KEY_FIELD_NAME, CISCO_MESSAGE_FIELD_NAME, null, CISCO_MESSAGE_GROK_PATTERNS_PATH);
    }

    @Test
    public void testMissingGrokPatternPath() {
        testParserConfigError(CISCO_KEY_FIELD_NAME, CISCO_MESSAGE_FIELD_NAME, "%{GREEDYDATA}", null);
    }

    @Test
    public void testGrokPathDoesNotExist() {
        String pathDoesNotExist = "/bad_path";
        testInvalidPath(pathDoesNotExist);
    }

    @Test
    public void testMalformedGrokPath() {
        String malformedPath = "malformed:path";
        testInvalidPath(malformedPath);
    }

    @Test
    public void testNullEmptyInputFieldName() {
        grokTableParser.inputField((String) null);
        assertEquals(Constants.DEFAULT_INPUT_FIELD, grokTableParser.getInputField().get());

        grokTableParser.inputField("     ");
        assertEquals(Constants.DEFAULT_INPUT_FIELD, grokTableParser.getInputField().get());

        FieldName fieldName = FieldName.of("    ");
        grokTableParser.inputField(fieldName);
        assertEquals(Constants.DEFAULT_INPUT_FIELD, grokTableParser.getInputField().get());

        grokTableParser.inputField((FieldName) null);
        assertEquals(Constants.DEFAULT_INPUT_FIELD, grokTableParser.getInputField().get());
    }

    private void testInvalidPath(String pathDoesNotExist) {
        grokTableParser.inputField(FieldName.of(INPUT_FIELD_NAME))
                .keyFieldName(CISCO_KEY_FIELD_NAME)
                .messageFieldName(CISCO_MESSAGE_FIELD_NAME)
                .initialGrokExpression("%{GREEDYDATA}");
        try {
            grokTableParser.grokPatternPath(pathDoesNotExist);
            fail("Expected illegal argument exception due to invalid grok pattern path.");
        } catch (IllegalArgumentException e) {
            Message input = Message.builder()
                    .addField(INPUT_FIELD_NAME, CISCO_LOG_MESSAGE)
                    .build();

            Message output = grokTableParser.parse(input);
            checkErrorMessage(output, String.format(GrokTableParser.PATTERN_LOADING_ERROR_FORMAT, pathDoesNotExist));
        }
    }

    private void testParserConfigError(String keyField, String messageFieldName, String initialGrokExpression, String grokPatternPath) {

        if (grokPatternPath != null) {
            grokPatternPath = getFileFromResource("/grok/cisco_asa").getAbsolutePath();
        }
        Message input = Message.builder()
                .addField(grokTableParser.getInputField(), CISCO_LOG_MESSAGE)
                .build();

        Message output = grokTableParser
                .grokPatternPath(grokPatternPath)
                .initialGrokExpression(initialGrokExpression)
                .keyFieldName(keyField)
                .messageFieldName(messageFieldName)
                .parse(input);

        List<String> expectedMissingFields = new ArrayList<>();
        addMissingConfigField(GrokTableParser.KEY_FIELD_NAME_CONFIGURATION, keyField, expectedMissingFields);
        addMissingConfigField(GrokTableParser.MESSAGE_FIELD_NAME_CONFIGURATION, messageFieldName, expectedMissingFields);
        addMissingConfigField(GrokTableParser.INITIAL_GROK_EXPRESSION_CONFIGURATION, initialGrokExpression, expectedMissingFields);
        addMissingConfigField(GrokTableParser.GROK_PATTERN_PATH_CONFIGURATION, grokPatternPath, expectedMissingFields);

        String expectedErrorMessage = String.format(GrokTableParser.MISSING_REQUIRED_CONFIGURATION_FIELD_ERROR_FORMAT, StringUtils.join(expectedMissingFields, ", "));
        checkErrorMessage(output, expectedErrorMessage);
    }

    private static void checkErrorMessage(Message output, String expectedErrorMessage) {
        assertTrue(output.getError().isPresent(), "Expected errors in parsing");
        assertThat(output.getError().get().getMessage(), is(expectedErrorMessage));
    }

    private void addMissingConfigField(String configName, String configFieldValue, List<String> missingFields) {
        if (configFieldValue == null) {
            missingFields.add(configName);
        }
    }

    private static File getFileFromResource(String path) {
        return new File(Objects.requireNonNull(GrokTableParser.class.getResource(path)).getFile());
    }
}
