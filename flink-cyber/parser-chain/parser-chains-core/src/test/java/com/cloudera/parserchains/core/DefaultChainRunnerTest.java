package com.cloudera.parserchains.core;

import com.cloudera.parserchains.core.model.define.ParserName;
import org.junit.jupiter.api.BeforeAll;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class DefaultChainRunnerTest {
    static String inputToParse;
    static Message errorMessage;
    static ParserName parserName;
    @Mock LinkName linkName1, linkName2;
    @Mock Parser parser1, parser2;

    @BeforeAll
    static void setup() {
        parserName = ParserName.of("Some Test Parser");
        inputToParse = "some input to parse";
        errorMessage = Message.builder()
                .withError("error")
                .createdBy(LinkName.of("parserInError", parserName))
                .build();
    }

    @Test
    void runChain() {
        ChainLink head = new NextChainLink(makeEchoParser(parser1), linkName1);
        ChainLink last = new NextChainLink(makeEchoParser(parser2), linkName2);
        head.setNext(last);
        List<Message> results = new DefaultChainRunner().run(inputToParse, head);

        // validate
        Message expected0 = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, inputToParse)
                .createdBy(DefaultChainRunner.ORIGINAL_MESSAGE_NAME)
                .build();
        Message expected1 = Message.builder()
                .clone(expected0)
                .createdBy(linkName1)
                .build();
        Message expected2 = Message.builder()
                .clone(expected1)
                .createdBy(linkName2)
                .build();
        assertThat("Expected 3 results, 1 original + 1 link1 + 1 link2.",
                results.size(), is(3));
        assertThat("Expected to see a result containing only the original message.",
                results, hasItem(expected0));
        assertThat("Expected to see a result from link1.",
                results, hasItem(expected1));
        assertThat("Expected to see a result from link2.",
                results, hasItem(expected2));
    }

    @Test
    void changeInputFieldName() {
        // change the name of the input field
        FieldName newInputField = FieldName.of("the_original_message");
        ChainLink chain = new NextChainLink(makeEchoParser(parser1), linkName1);
        List<Message> results = new DefaultChainRunner()
                .withInputField(newInputField)
                .run(inputToParse, chain);

        // validate
        Message expected0 = Message.builder()
                .addField(newInputField, FieldValue.of(inputToParse))
                .createdBy(DefaultChainRunner.ORIGINAL_MESSAGE_NAME)
                .build();
        Message expected1 = Message.builder()
                .clone(expected0)
                .createdBy(linkName1)
                .build();
        assertThat("Expected 2 results, 1 original + 1 link1.",
                results.size(), is(2));
        assertThat("Expected to see a result containing only the original message.",
                results, hasItem(expected0));
        assertThat("Expected to see a result from link1.",
                results, hasItem(expected1));
    }

    @Test
    void runChainWithError() {
        ChainLink head = new NextChainLink(makeErrorParser(parser1), linkName1);
        ChainLink next = new NextChainLink(parser2, linkName2);
        head.setNext(next);
        List<Message> results = new DefaultChainRunner().run(inputToParse, head);

        // validate
        Message expected0 = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, inputToParse)
                .createdBy(DefaultChainRunner.ORIGINAL_MESSAGE_NAME)
                .build();
        assertThat("Expected 2 results, 1 original + 1 link1.",
                results.size(), is(2));
        assertThat("Expected to see a result containing only the original message.",
                results, hasItem(expected0));
        assertThat("Expected link1 to report an error.",
                results.get(1).getError().isPresent());
        assertThat("Expected the result to be attributed to link1.",
                results.get(1).getCreatedBy(), is(linkName1));
    }

    @Test
    void nullChain() {
        DefaultChainRunner runner = new DefaultChainRunner();
        List<Message> results = runner.run(inputToParse, null);

        assertEquals(DefaultChainRunner.ORIGINAL_MESSAGE_NAME, results.get(0).getCreatedBy(),
                "Expected the 1st message to have 'createdBy' defined.");
        Message expectedMessage = Message.builder()
                .addField(runner.getInputField(), FieldValue.of(inputToParse))
                .createdBy(DefaultChainRunner.ORIGINAL_MESSAGE_NAME)
                .build();
        assertEquals(1, results.size(),
                "Expected 1 message to be returned to indicate there was an error.");
        assertTrue(results.get(0).getError().isPresent(),
                "Expected an error to be indicated on the message.");
    }
}
