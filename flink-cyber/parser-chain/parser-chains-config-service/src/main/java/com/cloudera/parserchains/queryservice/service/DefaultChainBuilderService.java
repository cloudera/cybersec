package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.ChainBuilder;
import com.cloudera.parserchains.core.ChainLink;
import com.cloudera.parserchains.core.InvalidParserException;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import org.springframework.stereotype.Service;

@Service
public class DefaultChainBuilderService implements ChainBuilderService {
    private ChainBuilder chainBuilder;

    public DefaultChainBuilderService(ChainBuilder chainBuilder) {
        this.chainBuilder = chainBuilder;
    }

    @Override
    public ChainLink build(ParserChainSchema chainSchema) throws InvalidParserException {
        return chainBuilder.build(chainSchema);
    }
}
