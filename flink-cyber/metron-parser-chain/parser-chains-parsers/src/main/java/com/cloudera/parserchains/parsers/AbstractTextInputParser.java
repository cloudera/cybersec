package com.cloudera.parserchains.parsers;

import java.nio.charset.StandardCharsets;

public abstract class AbstractTextInputParser extends AbstractParser {
    @Override
    public byte[] getTestBytes(String testTextToParse) {
        if (testTextToParse == null) {
            return null;
        } else {
            return testTextToParse.getBytes(StandardCharsets.UTF_8);
        }
    }
}
