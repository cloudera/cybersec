package com.cloudera.cyber.parser.wrappers;

import com.cloudera.cyber.parser.*;
import com.cloudera.parserchains.core.InvalidParserException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.cloudera.cyber.parser.wrappers.MessageFileParser.FILE_PATH_DID_NOT_MATCH_ANY_SPECIFIED_PATTERNS;
import static com.cloudera.cyber.parser.wrappers.MessageFileParser.UNMATCHED_FILE_SOURCE;
import static com.cloudera.cyber.parser.wrappers.SingleMessageParser.EMPTY_SIGNATURE;

public class MessageFileParserTest {

    @Test
    public void testSuccessful() throws NoSuchAlgorithmException, InvalidKeyException, InvalidParserException, IOException {
        ParserChainMap parserChainMap = ParserTestUtils.readParserChainMap("message_file/VpcFlowChain.json");

        SingleMessageParser singleMessageParser = new SingleMessageParser(parserChainMap, null);
        MessageFileParser parser = new MessageFileParser(singleMessageParser);

        MessageToParse messageToParse = MessageFileParserTestUtil.createMessageToParse("message_file/vpc_flow_samples.txt");
        ParserTestUtils.TestParserOutput parserOutput = new ParserTestUtils.TestParserOutput();
        parser.parse(new ParserChainSource("vpcflow", "netflow"), messageToParse, parserOutput);

        MessageFileParserTestUtil.verifyMessageFileOutput(parserOutput.getOutput(), messageToParse);
    }

    @Test
    public void nullParserChainIndicatingNoMatchError() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        testErrorCase("no_match_for_pattern", null, FILE_PATH_DID_NOT_MATCH_ANY_SPECIFIED_PATTERNS);
    }

    @Test
    public void fileDoesntExistError() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        testErrorCase("file_doesnt_exist", new ParserChainSource("vpcflow", "netflow"), "IOException with message file_doesnt_exist (No such file or directory)");
    }

    private static void testErrorCase(String filePath, ParserChainSource parserChainSource,String expectedErrorMessage) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        ParserChainMap parserChainMap = ParserTestUtils.readParserChainMap("message_file/VpcFlowChain.json");

        SingleMessageParser singleMessageParser = new SingleMessageParser(parserChainMap, null);
        MessageFileParser parser = new MessageFileParser(singleMessageParser);

        MessageToParse messageToParse = MessageFileParserTestUtil.createMessageToParse(filePath);
        ParserTestUtils.TestParserOutput parserOutput = new ParserTestUtils.TestParserOutput();
        String expectedSource = (parserChainSource != null) ? parserChainSource.getSource() : UNMATCHED_FILE_SOURCE;
        parser.parse(parserChainSource, messageToParse, parserOutput);

        MessageFileParserTestUtil.verifyErrorMessage(parserOutput, messageToParse, filePath, expectedSource, expectedErrorMessage, EMPTY_SIGNATURE);

    }
}
