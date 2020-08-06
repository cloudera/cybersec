package com.cloudera.parserchains.core;

/**
 * Parses a {@link Message}.
 *
 */
public interface Parser {

    /**
     * Parse a {@link Message}.
     *
     * <p>Parsers should not throw exceptions to indicate failure to parse conditions. Instead,
     * use {@link Message.Builder#withError}.
     *
     * @param message The message to parse.
     * @return A parsed message.
     */
    Message parse(Message message);
}
