package com.cloudera.cyber.parser.chain.resolver;

import com.cloudera.cyber.parser.*;
import com.cloudera.cyber.parser.wrappers.MessageFileParser;
import com.cloudera.parserchains.core.InvalidParserException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.cloudera.cyber.parser.wrappers.SingleMessageParser.EMPTY_SIGNATURE;

public class MessageFileParserChainResolverTest {

    @Test
    public void testSuccessful() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        TopicPatternToChainMap topicMap = ParserTestUtils.readTopicMap("message_file/VpcTopicMap.json");
        MessageFileParser parser = MessageFileParserTestUtil.createMessageFileParser();
        MessageFileParserChainResolver testChainResolver = new MessageFileParserChainResolver(topicMap.get("test_topic"), parser);
        ParserTestUtils.TestParserOutput parserOutput = new ParserTestUtils.TestParserOutput();
        MessageToParse messageToParse = MessageFileParserTestUtil.createMessageToParse("message_file/vpc_flow_samples.txt");
        testChainResolver.parse(messageToParse, parserOutput);
        MessageFileParserTestUtil.verifyMessageFileOutput(parserOutput.getOutput(), messageToParse);
    }

    @Test
    public void testNoPatternMatch() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidParserException {
        TopicPatternToChainMap topicMap = ParserTestUtils.readTopicMap("message_file/VpcTopicMap.json");
        MessageFileParser parser = MessageFileParserTestUtil.createMessageFileParser();
        MessageFileParserChainResolver testChainResolver = new MessageFileParserChainResolver(topicMap.get("test_topic"), parser);
        ParserTestUtils.TestParserOutput parserOutput = new ParserTestUtils.TestParserOutput();
        String fileToParse = "doesnt_match";
        MessageToParse messageToParse = MessageFileParserTestUtil.createMessageToParse(fileToParse);
        testChainResolver.parse(messageToParse, parserOutput);
        MessageFileParserTestUtil.verifyErrorMessage(parserOutput, messageToParse, fileToParse, MessageFileParser.UNMATCHED_FILE_SOURCE, MessageFileParser.FILE_PATH_DID_NOT_MATCH_ANY_SPECIFIED_PATTERNS,  EMPTY_SIGNATURE);
    }

}
