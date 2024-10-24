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
 * Routes message to one of many possible sub-chains based on values
 * contained with a {@link Message} matching a regular expression.
 */
public class RouterLink implements ChainLink {

    /**
     * Defines one route that a {@link Message} may take.
     */
    static class Route {
        final Regex regex;
        final ChainLink next;

        public Route(Regex regex, ChainLink next) {
            this.regex = Objects.requireNonNull(regex, "A valid regular expression is required.");
            this.next = Objects.requireNonNull(next, "A valid next link is required.");
        }
    }

    /**
     * The name of the field whose value is used for routing.
     */
    private FieldName inputField;
    private final List<Route> routes;
    private Optional<ChainLink> defaultRoute;
    private Optional<ChainLink> nextLink;

    public RouterLink() {
        this.routes = new ArrayList<>();
        this.defaultRoute = Optional.empty();
        this.nextLink = Optional.empty();
    }

    public RouterLink withInputField(FieldName fieldName) {
        inputField = Objects.requireNonNull(fieldName, "An input field must be defined.");
        return this;
    }

    public FieldName getInputField() {
        return inputField;
    }

    public RouterLink withRoute(Regex regex, ChainLink next) {
        routes.add(new Route(regex, next));
        return this;
    }

    List<Route> getRoutes() {
        return routes;
    }

    public RouterLink withDefault(ChainLink defaultNext) {
        this.defaultRoute = Optional.ofNullable(defaultNext);
        return this;
    }

    public Optional<ChainLink> getDefault() {
        return defaultRoute;
    }

    private Optional<ChainLink> findRoute(Message input) {
        if (inputField == null) {
            throw new IllegalStateException("The routing field was not defined.");
        }

        Optional<FieldValue> valueOpt = input.getField(inputField);
        if (valueOpt.isPresent()) {
            FieldValue fieldValue = valueOpt.get();

            for (Route route : routes) {
                Regex regex = route.regex;
                if (regex.matches(fieldValue)) {
                    return Optional.of(route.next);
                }
            }
        }

        // no routes matched, use the default next link if one is present
        return defaultRoute;
    }

    @Override
    public List<Message> process(Message input) {
        List<Message> results = new ArrayList<>();

        // route the message to the correct sub-chain
        findRoute(input).ifPresent(next -> {
            List<Message> nextResults = next.process(input);
            results.addAll(nextResults);
        });

        // retrieve the last output from the route taken;
        Message output = input;
        if (results.size() > 0) {
            output = results.get(results.size() - 1);
        }

        // if no errors, allow the next link in the chain to process the message
        boolean noError = !output.getError().isPresent();
        if (noError && nextLink.isPresent()) {
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
