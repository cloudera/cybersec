package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.ChainLink;
import com.cloudera.parserchains.queryservice.model.exec.ParserResult;

/**
 * A service that executes a parser chain on sample data.
 */
public interface ChainExecutorService {

    /**
     * Executes a parser chain by parsing a message.
     * @param chain The parser chain to execute.
     * @param textToParse The text to parse.
     * @return The result of parsing the text with the parser chain.
     */
    ParserResult execute(ChainLink chain, String textToParse);
}
