package com.cloudera.cyber.parser;

import com.cloudera.parserchains.core.ChainLink;
import com.cloudera.parserchains.core.DefaultChainRunner;
import com.cloudera.parserchains.core.LinkName;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.model.define.ParserName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Chain parser which only records and returns the final step of the parser chain
 *
 * Note that the default chain returns all steps of parsing
 */
public class NonRecordingChainRunner extends DefaultChainRunner {
    public static final LinkName ORIGINAL_MESSAGE_NAME = LinkName.of("original", ParserName.of("Test Parser Name"));
    private static final Logger logger = LogManager.getLogger(NonRecordingChainRunner.class);

    @Override
    public List<Message> run(String toParse, ChainLink chain) {
        List<Message> results = new ArrayList<>();
        Message original = originalMessage(toParse);
        try {
            List<Message> chainResults = chain.process(original);
            return Arrays.asList(chainResults.get(chainResults.size()-1));
        } catch(Throwable t) {
            String msg = "An unexpected error occurred while running a parser chain. " +
                    "Ensure that no parser is throwing an unchecked exception. Parsers should " +
                    "instead be reporting the error in the output message.";
            Message error = Message.builder()
                    .clone(original)
                    .withError(msg)
                    .build();
            results = Arrays.asList(error);

            logger.warn(msg, t);
            return results;
        }
    }
}
