package com.cloudera.parserchains.core;

import com.cloudera.parserchains.core.model.define.ParserChainSchema;

/**
 * Builds a parser chain from the definition of a parser chain; a {@link ParserChainSchema}.
 */
public interface ChainBuilder {

    /**
     * Builds a parser chain from a {@link ParserChainSchema} which defines
     * what a parser chain looks like.
     * @param chainSchema The blueprint for building the parser chain.
     * @return The first link in the parser chain.
     * @throws InvalidParserException If the user has defined an invalid parser chain.
     */
    ChainLink build(ParserChainSchema chainSchema) throws InvalidParserException;
}
