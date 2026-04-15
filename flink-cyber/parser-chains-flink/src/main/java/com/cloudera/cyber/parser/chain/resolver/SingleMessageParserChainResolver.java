package com.cloudera.cyber.parser.chain.resolver;

import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.cyber.parser.ParserChainSource;
import com.cloudera.cyber.parser.TopicParserConfig;
import com.cloudera.cyber.parser.wrappers.SingleMessageParser;

/**
 * Maps a single message parser to the parser chain and source to parse the raw message.
 */
public class SingleMessageParserChainResolver extends ParserChainResolver {
    private final ParserChainSource parserChainSource;

    /**
     * Constructor
     *
     * @param parserChainSource Topic map entry for this parser.
     * @param parser            The parser.
     */
    public SingleMessageParserChainResolver(TopicParserConfig parserChainSource, SingleMessageParser parser) {
        super(parser);
        this.parserChainSource = new ParserChainSource(parserChainSource.getChainKey(), parserChainSource.getSource(), null);
    }

    /**
     * Return the parser chain to parse this message and the source produced.
     *
     * @param message The raw message to resolve.
     * @return The parser chain and source configured for this message.
     */
    @Override
    protected ParserChainSource getParserChainSource(MessageToParse message) {
        return parserChainSource;
    }
}
