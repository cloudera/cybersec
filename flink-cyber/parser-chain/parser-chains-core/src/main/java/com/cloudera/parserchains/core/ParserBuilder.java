package com.cloudera.parserchains.core;

import com.cloudera.parserchains.core.catalog.ParserInfo;
import com.cloudera.parserchains.core.model.define.ParserSchema;

/**
 * Constructs a {@link Parser}.
 */
public interface ParserBuilder {

    /**
     * Constructs a {@link Parser} instance given the {@link ParserInfo} retrieved
     * from a {@link com.cloudera.parserchains.core.catalog.ParserCatalog}.
     * @param parserInfo Describes the parser to build.
     * @param parserSchema Describes how the parser should be configured.
     * @return
     */
    Parser build(ParserInfo parserInfo, ParserSchema parserSchema) throws InvalidParserException;
}
