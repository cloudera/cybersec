package com.cloudera.cyber.parser.chain.resolver;

import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.cyber.parser.ParserChainSource;
import com.cloudera.cyber.parser.wrappers.AbstractParserOutput;
import com.cloudera.cyber.parser.wrappers.ParserInterface;

/**
 * Abstract parent class for parser chain and source resolvers.
 */
public abstract class ParserChainResolver {
    /**
     * Resolved parser.
     */
    private final ParserInterface parser;

    /**
     * constructor
     *
     * @param parser Resolved parser.
     */
    public ParserChainResolver(ParserInterface parser) {
        this.parser = parser;
    }

    /**
     * Resolve the parser chain and source for a raw message.
     *
     * @param message The raw message to resolve.
     * @return The resolved parser chain and source to use when parsing the message.
     */
    protected abstract ParserChainSource getParserChainSource(MessageToParse message);

    /**
     * Parse the raw message and output the structured message.
     * If the message does not have quality errors
     * output to collector
     * else
     * output to error side output.
     *
     * @param message   The message to parse.
     * @param output   Sends parsed message on to the next processor.
     */
    public void parse(MessageToParse message, AbstractParserOutput output) {
        parser.parse(getParserChainSource(message), message, output);
    }
}
