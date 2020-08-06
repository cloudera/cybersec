package com.cloudera.parserchains.core;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class ChainLinkTestUtilities {

    /**
     * Create a mock parser that outputs the input that it receives; an echo.
     * @param mockParser The mock parser.
     */
    public static Parser makeEchoParser(Parser mockParser) {
        when(mockParser.parse(any()))
                .thenAnswer(i -> i.getArguments()[0]);
        return mockParser;
    }

    /**
     * Setup a mock parser to error during parsing.
     * @param mockParser The mock parser
     */
    public static Parser makeErrorParser(Parser mockParser) {
        when(mockParser.parse(any(Message.class)))
                .thenAnswer(i -> {
                    Message input = (Message) i.getArguments()[0];
                    return Message.builder()
                            .clone(input)
                            .withError("An error occurred.")
                            .build();
                });
        return mockParser;
    }

    /**
     * Creates a mock parser that adds a field to any message that it parses.
     * @param mockParser The mock parser.
     * @param fieldName The name of the field to add.
     * @param fieldValue The value of the field.
     */
    public static Parser makeParser(Parser mockParser, String fieldName, String fieldValue) {
        when(mockParser.parse(any(Message.class)))
                .thenAnswer(i -> {
                    Message input = (Message) i.getArguments()[0];
                    return Message.builder()
                            .clone(input)
                            .addField(fieldName, fieldValue)
                            .build();
                });
        return mockParser;
    }

    private ChainLinkTestUtilities() {
        // do not use
    }
}
