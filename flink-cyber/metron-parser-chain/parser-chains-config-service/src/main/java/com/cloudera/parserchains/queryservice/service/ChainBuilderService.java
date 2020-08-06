package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.ChainLink;
import com.cloudera.parserchains.core.InvalidParserException;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;

/**
 * A service that builds a parser chain from a parser chain schema.
 */
public interface ChainBuilderService {

    /**
     * Builds a parser chain from a {@link ParserChainSchema} which defines
     * what a parser chain looks like.
     * @param chainSchema The blueprint for building the parser chain.
     * @return The first link in the parser chain.
     * @throws InvalidParserException If the user has defined an invalid parser chain.
     */
    ChainLink build(ParserChainSchema chainSchema) throws InvalidParserException;
}
