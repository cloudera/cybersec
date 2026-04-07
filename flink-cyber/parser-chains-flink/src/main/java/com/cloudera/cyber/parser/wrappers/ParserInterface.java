package com.cloudera.cyber.parser.wrappers;

import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.cyber.parser.ParserChainSource;

/**
 * Base class for all parsers.
 */
public interface ParserInterface {

    /**
     * Parse a raw message and create a structured message.
     * If the parser detected data quality issues,
     *      send the message to the error side output.
     * else
     *      send to collector.
     *
     * @param parserChainSource The chain parser to use for parsing the message.
     * @param message The raw message to be parsed.
     * @param output The parser output that accepts a parsed message.
     */
    void parse(ParserChainSource parserChainSource, MessageToParse message, AbstractParserOutput output);

}

