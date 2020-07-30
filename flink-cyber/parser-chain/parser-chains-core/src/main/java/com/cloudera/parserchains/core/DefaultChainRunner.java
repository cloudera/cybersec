package com.cloudera.parserchains.core;

import com.cloudera.parserchains.core.model.define.ParserName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses a {@link Message} using a parser chain.
 */
public class DefaultChainRunner implements ChainRunner {
    public static final LinkName ORIGINAL_MESSAGE_NAME = LinkName.of("original", ParserName.of("Test Parser Name"));
    private static final Logger logger = LogManager.getLogger(DefaultChainRunner.class);
    private FieldName inputField;

    public DefaultChainRunner() {
        inputField = FieldName.of(Constants.DEFAULT_INPUT_FIELD);
    }

    /**
     * @param inputField The name of the field that is initialized with the raw input.
     */
    public DefaultChainRunner withInputField(FieldName inputField) {
        this.inputField = inputField;
        return this;
    }

    public FieldName getInputField() {
        return inputField;
    }

    @Override
    public List<Message> run(String toParse, ChainLink chain) {
        List<Message> results = new ArrayList<>();
        Message original = originalMessage(toParse);
        results.add(original);
        try {
            List<Message> chainResults = chain.process(original);
            results.addAll(chainResults);

        } catch(Throwable t) {
            String msg = "An unexpected error occurred while running a parser chain. " +
                    "Ensure that no parser is throwing an unchecked exception. Parsers should " +
                    "instead be reporting the error in the output message.";
            Message error = Message.builder()
                    .clone(original)
                    .withError(msg)
                    .build();
            results = Arrays.asList(error);
            logger.warn(msg, t);
        }
        return results;
    }

    @Override
    public Message originalMessage(String toParse) {
        return Message.builder()
                .addField(inputField, FieldValue.of(toParse))
                .createdBy(ORIGINAL_MESSAGE_NAME)
                .build();
    }
}
