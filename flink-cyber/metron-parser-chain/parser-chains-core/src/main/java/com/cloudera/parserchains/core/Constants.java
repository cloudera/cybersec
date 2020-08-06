package com.cloudera.parserchains.core;

public class Constants {
    /**
     * The default input field assumed by many parsers.
     * <p>When a parser chain is executed the text to parse is added to a field by this name.
     */
    public static final String DEFAULT_INPUT_FIELD = "original_string";

    private Constants() {
        // do not use
    }
}
