/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.parserchains.core;

import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.parserchains.core.model.define.ParserName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Parses a {@link Message} using a parser chain.
 */
@Slf4j
public class DefaultChainRunner implements ChainRunner {
    public static final LinkName ORIGINAL_MESSAGE_NAME = LinkName.of("original", ParserName.of("Test Parser Name"));
    private FieldName inputField;

    public DefaultChainRunner() {
        inputField = FieldName.of(Constants.DEFAULT_INPUT_FIELD);
    }

    /**
     * inputField setter.
     *
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
        Message original = originalMessage(toParse);
        return parseMessage(original, chain);
    }

    @Override
    public List<Message> run(Message original, ChainLink chain, List<Message> results) {
        try {
            List<Message> chainResults = chain.process(original);
            results.addAll(chainResults);
        } catch (Throwable t) {
            String msg = "An unexpected error occurred while running a parser chain. "
                         + "Ensure that no parser is throwing an unchecked exception. Parsers should "
                         + "instead be reporting the error in the output message.";
            results = getErrorResult(original, msg);
            log.warn(msg, t);
        }
        return results;
    }

    @Override
    public List<Message> run(MessageToParse toParse, ChainLink chain) {
        Message original = originalMessage(toParse);
        return parseMessage(original, chain);
    }

    private List<Message> parseMessage(Message original, ChainLink chain) {
        List<Message> results = new ArrayList<>();
        results.add(original);

        if (chain != null) {
            return run(original, chain, results);
        } else {
            return getErrorResult(original, "No parser chain defined for message");
        }
    }

    @Override
    public Message originalMessage(String toParse) {
        return Message.builder()
                      .addField(inputField, StringFieldValue.of(toParse))
                      .createdBy(ORIGINAL_MESSAGE_NAME)
                      .build();
    }

    @Override
    public Message originalMessage(MessageToParse toParse) {
        return Message.builder()
                      .addField(inputField, MessageToParseFieldValue.of(toParse))
                      .createdBy(ORIGINAL_MESSAGE_NAME)
                      .build();
    }

    private List<Message> getErrorResult(Message original, String errorMessage) {
        Message error = Message.builder()
                               .clone(original)
                               .withError(errorMessage)
                               .build();
        return Collections.singletonList(error);
    }
}
