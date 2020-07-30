package com.cloudera.parserchains.core;

import com.cloudera.parserchains.core.model.define.ParserName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.cloudera.parserchains.core.ChainLinkTestUtilities.makeEchoParser;
import static com.cloudera.parserchains.core.ChainLinkTestUtilities.makeErrorParser;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NextChainLinkTest {
    @Mock Parser parser1, parser2;
    static ParserName parserName = ParserName.of("Some Test Parser");
    static LinkName linkName1 = LinkName.of("parser1", parserName);
    static LinkName linkName2 = LinkName.of("parser2", parserName);
    static Message message = Message
            .builder()
            .createdBy(LinkName.of("original", parserName))
            .build();

    @Test
    void nextLink() {
        NextChainLink link1 = new NextChainLink(makeEchoParser(parser1), linkName1);
        NextChainLink link2 = new NextChainLink(makeEchoParser(parser2), linkName2);
        link1.setNext(link2);
        List<Message> results = link1.process(message);

        // validate
        Message expected1 = Message.builder()
                .clone(message)
                .createdBy(linkName1)
                .build();
        Message expected2 = Message.builder()
                .clone(message)
                .createdBy(linkName2)
                .build();
        assertThat("Expected 2 results, 1 result from each link.",
                results.size(), is(2));
        assertThat("Expected to see a result from link1.",
                results, hasItem(expected1));
        assertThat("Expected to see a result from link2.",
                results, hasItem(expected2));
    }

    @Test
    void noNextLink() {
        NextChainLink link1 = new NextChainLink(makeEchoParser(parser1), linkName1);
        List<Message> results = link1.process(message);

        // validate
        Message expected1 = Message.builder()
                .clone(message)
                .createdBy(linkName1)
                .build();
        assertThat("Expected only 1 result.",
                results.size(), is(1));
        assertThat("Expected to see a result from link1.",
                results, hasItem(expected1));
    }

    @Test
    void parsingError() {
        // the first link will trigger a parser error
        NextChainLink link1 = new NextChainLink(makeErrorParser(parser1), linkName1);
        NextChainLink link2 = new NextChainLink(parser2, linkName2);
        link1.setNext(link2);
        List<Message> results = link1.process(message);

        // validate
        assertThat("Expected only 1 result; processing stops on error.",
                results.size(), is(1));
        assertThat("Expected link1 to report an error.",
                results.get(0).getError().isPresent());
        assertThat("Expected the result to be attributed to link1.",
                results.get(0).getCreatedBy(), is(linkName1));
        verify(parser2, never().description("Expected no parsing to occur after the error in link1.")).parse(any());
    }

    @Test
    void parserReturnsNull() {
        Message input = Message.builder()
                .addField(FieldName.of("tag"), FieldValue.of("route1"))
                .createdBy(LinkName.of("original", parserName))
                .build();
        RouterLink routerLink = new RouterLink()
                .withInputField(FieldName.of("tag"))
                .withRoute(Regex.of("route1"), new NextChainLink(parser1, linkName1));

        // validate
        assertThrows(NullPointerException.class, () -> routerLink.process(input),
                "Expected an exception because the parser returns a null message.");
    }
}
