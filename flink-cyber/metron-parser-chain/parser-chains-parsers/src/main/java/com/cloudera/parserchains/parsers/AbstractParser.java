package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;

import java.util.Map;

public abstract class AbstractParser implements Parser {

    @Override
    public Message parse(Message input, Map<String, Object> metadataCache) {
        return parse(input);
    }
}
