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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link ChainLink} that links directly to the next link in a chain.
 */
public class NextChainLink implements ChainLink {
    private final Parser parser;
    private Optional<ChainLink> nextLink;
    private final LinkName linkName;

    /**
     * NextChainLink constructor.
     *
     * @param parser   The parser at this link in the chain.
     * @param linkName The name of this link in the chain.
     */
    public NextChainLink(Parser parser, LinkName linkName) {
        this.parser = Objects.requireNonNull(parser, "A valid parser is required.");
        this.nextLink = Optional.empty();
        this.linkName = Objects.requireNonNull(linkName, "A link name is required.");
    }

    @Override
    public List<Message> process(Message input) {
        // parse the input message
        Message parsed = parser.parse(input);
        Objects.requireNonNull(parsed, "Parser must not return a null message.");

        boolean emitMessage = parsed.getEmit();

        // ensure the message is attributed to this link by name
        Message output = Message.builder()
                                .clone(parsed)
                                .createdBy(linkName)
                                .emit(emitMessage)
                                .build();
        List<Message> results = new ArrayList<>();
        results.add(output);

        // if no errors, allow the next link in the chain to process the message
        boolean noError = !output.getError().isPresent();
        if (noError && emitMessage && nextLink.isPresent()) {
            List<Message> nextResults = nextLink.get().process(output);
            results.addAll(nextResults);
        }
        return results;
    }

    @Override
    public void setNext(ChainLink nextLink) {
        this.nextLink = Optional.of(nextLink);
    }
}
