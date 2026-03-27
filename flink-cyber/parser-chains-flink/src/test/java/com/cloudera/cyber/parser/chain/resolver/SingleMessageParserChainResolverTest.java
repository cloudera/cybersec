package com.cloudera.cyber.parser.chain.resolver;

import com.cloudera.cyber.parser.*;
import com.cloudera.cyber.parser.wrappers.SingleMessageParser;
import com.cloudera.parserchains.core.InvalidParserException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;

public class SingleMessageParserChainResolverTest {

    @Test
    public void testResolutionToSingleMessageParser() throws NoSuchAlgorithmException, InvalidKeyException, InvalidParserException, IOException {
        SingleMessageParser testParser = SingleMessageParserTestUtil.createParser("simple_message_chain.json", null);

        ParserChainResolver resolver = new SingleMessageParserChainResolver(new TopicParserConfig("simple_message", "simple", "", null), testParser);

        Map<String, String> expectedExtensions = SingleMessageParserTestUtil.createExtensions("500", "1.2.3.4");
        long expectedTimestamp = Instant.now().toEpochMilli();
        MessageToParse messageToParse = SingleMessageParserTestUtil.createGoodMessage( expectedTimestamp, expectedExtensions);

        ParserTestUtils.TestParserOutput testParserOutput = new ParserTestUtils.TestParserOutput();
        resolver.parse(messageToParse, testParserOutput);

        // check the extracted fields to make sure the message was routed to the right parser
        SingleMessageParserTestUtil.verifySuccessfulParserOutput(testParserOutput, messageToParse, expectedTimestamp, "simple", expectedExtensions, SingleMessageParser.EMPTY_SIGNATURE);
    }

}
