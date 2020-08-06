package com.cloudera.parserchains.core;

import java.util.List;

/**
 * One link in a parser chain.
 */
public interface ChainLink {

    /**
     * Parses an message starting at this link in the chain.
     * <p>This involves all of the downstream links in this chain, not just this chain link.
     * @param input The input message to parse.
     * @return One {@link Message} for every link in the parser chain.
     */
    List<Message> process(Message input);

    /**
     * Define the next link in the chain.
     * <p>If not defined, this is the last link in the parser chain.
     * @param nextLink The next chain link.
     */
    void setNext(ChainLink nextLink);
}
