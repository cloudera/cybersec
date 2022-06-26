package com.cloudera.parserchains.core;

import java.util.*;

/**
 * A {@link ChainLink} that links directly to the next link in a chain.
 */
public class NextChainLink implements ChainLink {
    private Parser parser;
    private Optional<ChainLink> nextLink;
    private LinkName linkName;

    /**
     * @param parser The parser at this link in the chain.
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
