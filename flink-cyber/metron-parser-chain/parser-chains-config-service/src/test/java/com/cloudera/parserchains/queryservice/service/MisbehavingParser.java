package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.MessageParser;

/**
 * This parser is used for testing only.  This will throw an unchecked exception, something that
 * parsers should NOT do.
 */
@MessageParser(
        name = "Misbehaving Parser",
        description = "A parser used for testing only.")
public class MisbehavingParser implements Parser {

    @Override
    public Message parse(Message message) {
        throw new RuntimeException("No parser should throw unchecked exceptions, but what if?");
    }
}
