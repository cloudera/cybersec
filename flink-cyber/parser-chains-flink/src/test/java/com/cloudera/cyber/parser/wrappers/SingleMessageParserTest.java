package com.cloudera.cyber.parser.wrappers;

import com.cloudera.cyber.parser.*;
import com.cloudera.parserchains.core.InvalidParserException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

public class SingleMessageParserTest {
    private static Signature signature;

    @BeforeAll
    public static void createSignature() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InvalidKeyException {
        signature = Signature.getInstance("SHA1WithRSA");
        signature.initSign(ParserTestUtils.loadPrivateKey());
    }

    @Test
    public void testSuccessfulSignedMessage() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException, InvalidKeyException, InvalidParserException, SignatureException {
        testSuccessfulMessage(ParserTestUtils.loadPrivateKey());
    }

    @Test
    public void testSuccessfulUnsignedMessage() throws NoSuchAlgorithmException, IOException, InvalidKeyException, InvalidParserException, SignatureException {
        testSuccessfulMessage(null);
    }

    @Test
    public void testChainDoesntExist() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        SingleMessageParser testParser = SingleMessageParserTestUtil.createParser("simple_message_chain.json", null);

        MessageToParse messageToParse = SingleMessageParserTestUtil.createMessageToParse(String.format("%d 455 1.2.3.4", Instant.now().toEpochMilli()));
        ParserTestUtils.TestParserOutput testParserOutput = new ParserTestUtils.TestParserOutput();

        final String testSource = "test_source";
        // Parser chain source references a chain that doesn't exist
        testParser.parse(new ParserChainSource("DOES_NOT_EXIST", testSource), messageToParse, testParserOutput);
        SingleMessageParserTestUtil.verifyErrorParserOutput(testParserOutput, messageToParse, testSource, "No parser chain defined for message");
    }

    @Test
    public void testFilteredMessage() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        SingleMessageParser testParser = SingleMessageParserTestUtil.createParser("metron/parser_chain.json", null);
        MessageToParse messageToParse = SingleMessageParserTestUtil.createMessageToParse(ParserTestUtils.readConfigFile("metron/samples/oraclelogon_filtered.txt"));
        ParserTestUtils.TestParserOutput testParserOutput = new ParserTestUtils.TestParserOutput();
        testParser.parse(new ParserChainSource("oraclelogon", "oraclelogon"), messageToParse, testParserOutput);
        // no output message because the metron parser is configured to filter out messages
        assertThat(testParserOutput.getOutput().isEmpty()).isTrue();
    }

    @Test
    public void testInvalidParserChain() throws IOException {
        ParserChainMap chains = ParserTestUtils.readParserChainMap("metron/parser_chain_invalid.json");
        assertThatThrownBy(() -> SingleMessageParser.create(chains, null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining(String.format("The following parser chains did not parse: %s", String.join(", ", chains.keySet())));

        chains.putAll(ParserTestUtils.readParserChainMap("ErrorParserChain.json"));
        assertThatThrownBy(() -> SingleMessageParser.create(chains, null)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("The following parser chains did not parse: %s", String.join(", ", chains.keySet()));
    }

    @Test
    public void testInvalidNonNumericTimestamp() throws NoSuchAlgorithmException, InvalidKeyException, InvalidParserException, IOException {
        SingleMessageParser testParser = SingleMessageParserTestUtil.createParser("simple_message_chain.json", null);

        String elapsed = "455";
        String ipSrcAddr = "1.2.3.4";
        // non-numeric timestamp - grok pattern succeeds but timestamp parser fails to extract
        MessageToParse messageToParse = SingleMessageParserTestUtil.createMessageToParse(String.format("ABCD %s %s", elapsed, ipSrcAddr));

        ParserTestUtils.TestParserOutput testParserOutput = new ParserTestUtils.TestParserOutput();

        final String testSource = "test_source";
        testParser.parse(new ParserChainSource("simple_message", testSource), messageToParse, testParserOutput);

        Map<String, String> extensionsFromGrok = SingleMessageParserTestUtil.createExtensions(elapsed, ipSrcAddr);
        SingleMessageParserTestUtil.verifyErrorParserOutput(testParserOutput, messageToParse, extensionsFromGrok, testSource, "Timestamp is not in epoch milliseconds or seconds. For input string: \"ABCD\"");
    }

    @Test
    public void testMessageFailsGrok() throws NoSuchAlgorithmException, InvalidKeyException, InvalidParserException, IOException {
        SingleMessageParser testParser = SingleMessageParserTestUtil.createParser("simple_message_chain.json", null);

        // message with only timestamp fails grok parser
        MessageToParse messageToParse = SingleMessageParserTestUtil.createMessageToParse(String.format("%d", Instant.now().toEpochMilli()));

        ParserTestUtils.TestParserOutput testParserOutput = new ParserTestUtils.TestParserOutput();

        final String testSource = "test_source";
        testParser.parse(new ParserChainSource("simple_message", testSource), messageToParse, testParserOutput);
        SingleMessageParserTestUtil.verifyErrorParserOutput(testParserOutput, messageToParse, testSource, SingleMessageParser.NO_TIMESTAMP_FIELD_MESSAGE);
    }

    @Test
    public void testMessageWithShortTimestamp() throws NoSuchAlgorithmException, InvalidKeyException, InvalidParserException, IOException, SignatureException {
        long inputTimestamp = 1234;
        // test adjustment of timestamp to epoch millis
        testSuccessfulMessage(inputTimestamp, inputTimestamp * 1000, null);
    }

    private void testSuccessfulMessage(PrivateKey signKey) throws IOException, InvalidParserException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        long expectedTimestamp = Instant.now().toEpochMilli();
        testSuccessfulMessage(expectedTimestamp, expectedTimestamp, signKey);
    }

    private void testSuccessfulMessage(long inputTimestamp, long expectedOutputTimestamp, PrivateKey signKey) throws NoSuchAlgorithmException, IOException, InvalidKeyException, InvalidParserException, SignatureException {
        SingleMessageParser testParser = SingleMessageParserTestUtil.createParser("simple_message_chain.json", signKey);
        Map<String, String> expectedExtensions = SingleMessageParserTestUtil.createExtensions("400", "1.2.3.4");

        MessageToParse messageToParse = SingleMessageParserTestUtil.createGoodMessage(inputTimestamp, expectedExtensions);
        ParserTestUtils.TestParserOutput testParserOutput = new ParserTestUtils.TestParserOutput();

        final String testSource = "test_source";
        testParser.parse(new ParserChainSource("simple_message", testSource), messageToParse, testParserOutput);
        SingleMessageParserTestUtil.verifySuccessfulParserOutput(testParserOutput, messageToParse, expectedOutputTimestamp, testSource, expectedExtensions, signMessage(messageToParse, signKey));
    }

    private byte[] signMessage(MessageToParse messageToParse, PrivateKey privateKey) throws SignatureException {
        if (privateKey != null) {
            byte[] bytes = messageToParse.getOriginalBytes();

            signature.update(bytes);
            return signature.sign();
        } else {
            return SingleMessageParser.EMPTY_SIGNATURE;
        }
    }

}
