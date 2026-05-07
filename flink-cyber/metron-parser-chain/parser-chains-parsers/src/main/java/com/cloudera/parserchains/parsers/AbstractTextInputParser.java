package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Parser;

import java.nio.charset.StandardCharsets;

public abstract class AbstractTextInputParser implements Parser {
    @Override
    public byte[] getTestBytes(String testTextToParse) {
        if (testTextToParse == null) {
            return null;
        } else {
            return testTextToParse.getBytes(StandardCharsets.UTF_8);
        }
    }
}
