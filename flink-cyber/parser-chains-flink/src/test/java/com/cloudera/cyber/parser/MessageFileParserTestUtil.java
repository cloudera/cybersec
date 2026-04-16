package com.cloudera.cyber.parser;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.parser.wrappers.MessageFileParser;
import com.cloudera.cyber.parser.wrappers.SingleMessageParser;
import com.cloudera.parserchains.core.Constants;
import com.cloudera.parserchains.core.InvalidParserException;
import com.google.common.io.Resources;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloudera.cyber.parser.wrappers.MessageFileParser.MESSAGE_SOURCE_FILE_STATUS;
import static com.cloudera.cyber.parser.wrappers.SingleMessageParser.EMPTY_SIGNATURE;
import static com.cloudera.parserchains.core.Constants.DEFAULT_ORIGINAL_FILE_LINE_FIELD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class MessageFileParserTestUtil {

    private static final String VALID_MESSAGE_FILE_PATH = "message_file";

    public static String getValidMessageFileAllowedPath() {
        return ParserTestUtils.resolveResourcePath(VALID_MESSAGE_FILE_PATH);
    }

    public static MessageFileParser createMessageFileParser() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        ParserChainMap chains = ParserTestUtils.readParserChainMap(String.format("%s/VpcFlowChain.json", VALID_MESSAGE_FILE_PATH));
        List<String> allowedPaths = Collections.singletonList(getValidMessageFileAllowedPath());
        return MessageFileParser.create(allowedPaths, SingleMessageParser.create(chains, null));
    }

    public static MessageToParse createMessageToParse(String filename) {
        String sampleFile;

        try {
            sampleFile = Resources.getResource(filename).getPath();
        } catch (Exception e) {
            // if the file doesn't exist, it's an error case
            sampleFile = filename;
        }
        return MessageToParse.builder().topic("test_topic").offset(3).partition(100).
                originalBytes(sampleFile.getBytes(StandardCharsets.UTF_8)).build();
    }

    public static void verifyMessageFileOutput(List<Message> messages, MessageToParse messageToParse) throws IOException {
        verifyMessageFileOutput(messages, messageToParse, 1);
    }

    public static void verifyMessageFileOutput(List<Message> messages, MessageToParse messageToParse, int messageStartLine) throws IOException {
        assertThat(messages.size()).isEqualTo(3);
        for (Message actualMessage : messages) {
            Map<String, String> extensions = actualMessage.getExtensions();
            if (!actualMessage.getSource().equals(MESSAGE_SOURCE_FILE_STATUS)) {
                assertThat(extensions.size()).isEqualTo(22);
                SignedSourceKey expectedOriginalSource = ParserTestUtils.createExpectedOriginalSource(messageToParse, EMPTY_SIGNATURE);
                assertThat(actualMessage.getOriginalSource()).isEqualTo(expectedOriginalSource);
                long originalLineNumber = Long.parseLong(extensions.get(DEFAULT_ORIGINAL_FILE_LINE_FIELD));
                if (originalLineNumber == messageStartLine) {
                    assertThat(extensions.get("netflow_flow_direction")).isEqualTo("ingress");
                } else if (originalLineNumber == messageStartLine + 1) {
                    assertThat(extensions.get("netflow_flow_direction")).isEqualTo("egress");
                } else {
                    fail(String.format("line number %d extension is out of range ", originalLineNumber));
                }
            } else {
                String originalFilePath = new String(messageToParse.getOriginalBytes());
                long modificationTime = Files.getLastModifiedTime(Paths.get(originalFilePath)).toMillis();
                Map<String, String> expectedExtensions = new HashMap<>();
                expectedExtensions.put("filePath", new String(messageToParse.getOriginalBytes()));
                expectedExtensions.put("modificationTime", String.valueOf(modificationTime));
                expectedExtensions.put("successMessageCount", String.valueOf(2));
                expectedExtensions.put("errorMessageCount", String.valueOf(0));
                if (messageStartLine > 1) {
                    expectedExtensions.put("headerLines", String.valueOf(messageStartLine - 1));
                }
                assertThat(extensions).isEqualTo(expectedExtensions);
            }
        }
    }

    public static void verifyErrorMessage(ParserTestUtils.TestParserOutput parserOutput, MessageToParse messageToParse, String fileToParse, String expectedSource, String expectedErrorMessage, byte[] signature ) {
        assertThat(parserOutput.getOutput().size()).isEqualTo(1);
        Message errorMessage = parserOutput.getOutput().get(0);
        assertThat(errorMessage.getSource()).isEqualTo(expectedSource);
        Map<String, String> expectedExtensions = new HashMap<>();
        expectedExtensions.put(Constants.DEFAULT_INPUT_FIELD, fileToParse);

        assertThat(errorMessage.getExtensions()).isEqualTo(expectedExtensions);
        assertThat(errorMessage.getDataQualityMessages().size()).isEqualTo(1);
        assertThat(errorMessage.getOriginalSource()).isEqualTo(
                ParserTestUtils.createExpectedOriginalSource(messageToParse, signature));
        DataQualityMessage dqMessage = errorMessage.getDataQualityMessages().get(0);
        assertThat(dqMessage).isEqualTo(DataQualityMessage.builder().
                message(expectedErrorMessage).
                level(DataQualityMessageLevel.ERROR.name()).
                feature(MessageFileParser.MESSAGE_FILE_PARSER_FEATURE).
                field(Constants.DEFAULT_INPUT_FIELD).
                build());
    }
}
