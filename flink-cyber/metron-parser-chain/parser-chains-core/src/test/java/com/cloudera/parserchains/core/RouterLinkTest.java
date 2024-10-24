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

import static com.cloudera.parserchains.core.ChainLinkTestUtilities.makeEchoParser;
import static com.cloudera.parserchains.core.ChainLinkTestUtilities.makeErrorParser;
import static com.cloudera.parserchains.core.ChainLinkTestUtilities.makeParser;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.cloudera.parserchains.core.model.define.ParserName;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RouterLinkTest {
    private ParserName parserName;
    @Mock Parser parser1, parser2, parser3;
    @Mock LinkName linkName1, linkName2, linkName3;

    @BeforeEach
    public void setup() {
        parserName = ParserName.of("Some Test Parser");
    }

    @Test
    void route() {
        Message input = Message.builder()
                .addField(FieldName.of("tag"), StringFieldValue.of("route1"))
                .createdBy(LinkName.of("original", parserName))
                .build();
        ChainLink route1 = new NextChainLink(makeParser(parser1, "route", "route1"), linkName1);
        ChainLink route2 = new NextChainLink(makeParser(parser2, "route", "route2"), linkName2);
        RouterLink routerLink = new RouterLink()
                .withInputField(FieldName.of("tag"))
                .withRoute(Regex.of("route1"), route1)
                .withRoute(Regex.of("route2"), route2);
        List<Message> results = routerLink.process(input);

        // validate
        Message expectedRoute1 = Message.builder()
                .clone(input)
                .addField("route", "route1")
                .createdBy(linkName1)
                .build();
        assertThat("Expected 'route1' to have been followed.",
                results, hasItem(expectedRoute1));
        assertThat("Expected 1 result from the route that was followed.",
                results.size(), is(1));
    }

    @Test
    void noRoute() {
        Message input = Message.builder()
                .addField(FieldName.of("tag"), StringFieldValue.of("no_match"))
                .createdBy(LinkName.of("original", parserName))
                .build();
        ChainLink route1 = new NextChainLink(parser1, linkName1);
        ChainLink route2 = new NextChainLink(parser2, linkName2);
        RouterLink routerLink = new RouterLink()
                .withInputField(FieldName.of("tag"))
                .withRoute(Regex.of("route1"), route1)
                .withRoute(Regex.of("route2"), route2);
        List<Message> results = routerLink.process(input);

        // validate
        assertThat("Expected 0 results since there is no valid route to take.",
                results.size(), is(0));
    }

    /**
     * What happens when a message follows a route and then also must go to the next chain link?
     */
    @Test
    void routeWithNextLink() {
        Message input = Message.builder()
                .addField(FieldName.of("tag"), StringFieldValue.of("route1"))
                .createdBy(LinkName.of("original", parserName))
                .build();
        ChainLink route1 = new NextChainLink(makeParser(parser1, "route1", "route1"), linkName1);
        ChainLink route2 = new NextChainLink(makeParser(parser2, "route2", "route2"), linkName2);
        ChainLink nextLink = new NextChainLink(makeParser(parser3, "nextLink", "nextLink"), linkName3);
        RouterLink routerLink = new RouterLink()
                .withInputField(FieldName.of("tag"))
                .withRoute(Regex.of("route1"), route1)
                .withRoute(Regex.of("route2"), route2);
        routerLink.setNext(nextLink);
        List<Message> results = routerLink.process(input);

        // validate
        Message expectedRoute1 = Message.builder()
                .clone(input)
                .addField("route1", "route1")
                .createdBy(linkName1)
                .build();
        Message expectedNext = Message.builder()
                .clone(expectedRoute1)
                .addField("nextLink", "nextLink")
                .createdBy(linkName3)
                .build();
        assertThat("Expected 'route1' to have been followed.",
                results, hasItem(expectedRoute1));
        assertThat("Expected the next link to have been followed.",
                results, hasItem(expectedNext));
        assertThat("Expected 2 results; 1 from route1 and 1 from the next chain link.",
                results.size(), is(2));
    }

    /**
     * What happens when a message has no route, but then must go to the next chain link?
     */
    @Test
    void noRouteWithNextLink() {
        Message input = Message.builder()
                .addField(FieldName.of("tag"), StringFieldValue.of("route1"))
                .createdBy(LinkName.of("original", parserName))
                .build();
        ChainLink route1 = new NextChainLink(makeEchoParser(parser1), linkName1);
        ChainLink route2 = new NextChainLink(makeEchoParser(parser2), linkName2);
        RouterLink routerLink = new RouterLink()
                .withInputField(FieldName.of("tag"))
                .withRoute(Regex.of("route1"), route1)
                .withRoute(Regex.of("route2"), route2);
        routerLink.setNext(new NextChainLink(makeEchoParser(parser3), linkName3));
        List<Message> results = routerLink.process(input);

        // validate
        Message expectedNext = Message.builder()
                .clone(input)
                .createdBy(linkName1)
                .build();
        assertThat("Expected the next link to have been followed.",
                results, hasItem(expectedNext));
        assertThat("Expected 1 result from the next link in the chain.",
                results.size(), is(2));
    }

    /**
     * What happens when a message has a route that causes an error, but also has a next link?
     */
    @Test
    void errorRouteWithNextLink() {
        Message input = Message.builder()
                .addField(FieldName.of("tag"), StringFieldValue.of("route1"))
                .createdBy(LinkName.of("original", parserName))
                .build();
        ChainLink route1 = new NextChainLink(makeErrorParser(parser1), linkName1);
        RouterLink routerLink = new RouterLink()
                .withInputField(FieldName.of("tag"))
                .withRoute(Regex.of("route1"), route1);
        routerLink.setNext(new NextChainLink(makeEchoParser(parser2), linkName2));
        List<Message> results = routerLink.process(input);

        // validate
        assertThat("Expected 1 result showing the error caused by the route taken. "
                        + "The next link to parser2 should not be followed.",
                results.size(), is(1));

    }

    @Test
    void defaultRoute() {
        Message input = Message.builder()
                .addField(FieldName.of("tag"), StringFieldValue.of("use_default"))
                .createdBy(LinkName.of("original", parserName))
                .build();
        ChainLink route1 = new NextChainLink(makeEchoParser(parser1), linkName1);
        ChainLink route2 = new NextChainLink(makeEchoParser(parser2), linkName2);
        ChainLink route3 = new NextChainLink(makeEchoParser(parser3), linkName3);
        RouterLink routerLink = new RouterLink()
                .withInputField(FieldName.of("tag"))
                .withRoute(Regex.of("route1"), route1)
                .withRoute(Regex.of("route2"), route2)
                .withDefault(route3);
        List<Message> results = routerLink.process(input);

        // validate
        Message expectedDefaultRoute = Message.builder()
                .clone(input)
                .createdBy(linkName3)
                .build();
        assertThat("Expected 1 result from the default route.",
                results.size(), is(1));
        assertThat("Expected default route to have been followed.",
                results, hasItem(expectedDefaultRoute));
    }

    @Test
    void undefinedRoutingField() {
        Message input = Message.builder()
                .addField(FieldName.of("tag"), StringFieldValue.of("use_default"))
                .createdBy(LinkName.of("original", parserName))
                .build();
        ChainLink route1 = new NextChainLink(makeEchoParser(parser1), linkName1);
        ChainLink route2 = new NextChainLink(makeEchoParser(parser2), linkName2);
        ChainLink route3 = new NextChainLink(makeEchoParser(parser3), linkName3);
        RouterLink routerLink = new RouterLink()
                // do not define the input field here
                .withRoute(Regex.of("route1"), route1)
                .withRoute(Regex.of("route2"), route2)
                .withDefault(route3);

        // validate
        Assertions.assertThrows(IllegalStateException.class, () -> routerLink.process(input),
                "Expected an exception since the input field was not defined.");
    }

    @Test
    void parserReturnsNull() {
        // the mock 'parser1' will return a null message
        when(parser1.parse(any())).thenReturn(null);
        ChainLink route1 = new NextChainLink(parser1, linkName1);
        Message input = Message.builder()
                .addField(FieldName.of("tag"), StringFieldValue.of("route1"))
                .createdBy(LinkName.of("original", parserName))
                .build();
        RouterLink routerLink = new RouterLink()
                .withInputField(FieldName.of("tag"))
                .withRoute(Regex.of("route1"), route1);

        // validate
        assertThrows(NullPointerException.class, () -> routerLink.process(input),
                "Expected an exception because the parser returns a null message.");
    }
}
