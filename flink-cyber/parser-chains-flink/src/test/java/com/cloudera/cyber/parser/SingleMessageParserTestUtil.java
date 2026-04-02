package com.cloudera.cyber.parser;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.parser.wrappers.SingleMessageParser;
import com.cloudera.parserchains.core.Constants;
import com.cloudera.parserchains.core.InvalidParserException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.cloudera.cyber.parser.wrappers.SingleMessageParser.EMPTY_SIGNATURE;
import static com.cloudera.parserchains.core.Constants.DEFAULT_ORIGINAL_FILE_LINE_FIELD;
import static org.assertj.core.api.Assertions.assertThat;

public class SingleMessageParserTestUtil {
    private static final String ELAPSED_EXTENSION = "elapsed";
    private static final String IP_SRC_ADDR = "ip_src_addr";

    public static Map<String, String> createExtensions(String elapsed, String ipSrcAddr) {
        Map<String, String> extensions = new HashMap<>();
        extensions.put(DEFAULT_ORIGINAL_FILE_LINE_FIELD, "-1");
        extensions.put(ELAPSED_EXTENSION, elapsed);
        extensions.put(IP_SRC_ADDR, ipSrcAddr);

        return extensions;
    }

    public static SingleMessageParser createParser(String chainFile, PrivateKey signKey) throws NoSuchAlgorithmException, InvalidKeyException, InvalidParserException, IOException {
        ParserChainMap chains = ParserTestUtils.readParserChainMap(chainFile);
        return SingleMessageParser.create(chains, signKey);
    }

    public static MessageToParse createMessageToParse(String rawMessage) {
        return MessageToParse.builder().offset(100).partition(3).originalBytes(rawMessage.getBytes(StandardCharsets.UTF_8)).build();
    }

    public static MessageToParse createGoodMessage(long timestamp, Map<String, String> extensions) {
        String rawMessage = String.format("%d %s %s", timestamp, extensions.get("elapsed"), extensions.get("ip_src_addr"));
        return createMessageToParse(rawMessage);
    }

    public static void verifySuccessfulParserOutput(ParserTestUtils.TestParserOutput parserOutput, MessageToParse messageToParse, long expectedTimestamp, String expectedSource, Map<String, String> expectedExtensions, byte[] expectedSignature) {
        assertThat(parserOutput.getOutput().size()).isEqualTo(1);
        Message message = parserOutput.getOutput().get(0);
        verifyMessageContent(messageToParse, expectedTimestamp, expectedSource, expectedExtensions, expectedSignature, message);
        assertThat(message.getDataQualityMessages()).isNull();
    }

    private static void verifyMessageContent(MessageToParse messageToParse, long expectedTimestamp, String expectedSource, Map<String, String> expectedExtensions, byte[] expectedSignature, Message message) {
        assertThat(message.getOriginalSource()).isEqualTo(ParserTestUtils.createExpectedOriginalSource(messageToParse, expectedSignature));
        assertThat(message.getTs()).isEqualTo(expectedTimestamp);
        assertThat(message.getSource()).isEqualTo(expectedSource);
        assertThat(message.getExtensions()).isEqualTo(expectedExtensions);
    }

    public static void verifyErrorParserOutput(ParserTestUtils.TestParserOutput parserOutput, MessageToParse messageToParse, String expectedSource, String expectedErrorMessage) {
        verifyErrorParserOutput(parserOutput, messageToParse, Collections.emptyMap(), expectedSource, expectedErrorMessage);
    }

    public static void verifyErrorParserOutput(ParserTestUtils.TestParserOutput parserOutput, MessageToParse messageToParse, Map<String, String> extraExtensions, String expectedSource, String expectedErrorMessage) {
        Map<String, String> expectedExtensions = new HashMap<>();
        expectedExtensions.put(Constants.DEFAULT_INPUT_FIELD, new String(messageToParse.getOriginalBytes()));
        expectedExtensions.put(Constants.DEFAULT_ORIGINAL_FILE_LINE_FIELD, "-1");
        expectedExtensions.putAll(extraExtensions);

        assertThat(parserOutput.getOutput().size()).isEqualTo(1);
        Message message = parserOutput.getOutput().get(0);
        verifyMessageContent(messageToParse, message.getTs(), expectedSource, expectedExtensions, EMPTY_SIGNATURE, message);
        assertThat(message.getDataQualityMessages().get(0)).isEqualTo(
                DataQualityMessage.builder().feature(SingleMessageParser.CHAIN_PARSER_FEATURE).
                        field(Constants.DEFAULT_INPUT_FIELD).level(DataQualityMessageLevel.ERROR.name()).
                        message(expectedErrorMessage).build());
    }
}
