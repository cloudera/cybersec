package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Message;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JSONParserTest {
    JSONParser jsonParser;

    @BeforeEach
    void beforeEach() {
        jsonParser = new JSONParser();
    }

    /**
     * {
     *    "name":"Cake",
     *    "type":"Donut",
     *    "ppu":0.55,
     *    "batters":{
     *       "batter":[
     *          {
     *             "id":"1001",
     *             "type":"Regular"
     *          },
     *          {
     *             "id":"1002",
     *             "type":"Chocolate"
     *          }
     *       ]
     *    },
     *    "topping":[
     *       {
     *          "id":"5001",
     *          "type":"None"
     *       },
     *       {
     *          "id":"5002",
     *          "type":"Glazed"
     *       }
     *    ]
     * }
     */
    @Multiline
    static String jsonToParse;

    @Test
    void byDefault() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, jsonToParse)
                .build();
        Message output = jsonParser
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("name", "Cake")
                .addField("type", "Donut")
                .addField("ppu", "0.55")
                .addField("batters.batter[0].id", "1001")
                .addField("batters.batter[0].type", "Regular")
                .addField("batters.batter[1].id", "1002")
                .addField("batters.batter[1].type", "Chocolate")
                .addField("topping[0].id", "5001")
                .addField("topping[0].type", "None")
                .addField("topping[1].id", "5002")
                .addField("topping[1].type", "Glazed")
                .build();
        assertThat(output, is(expected));
    }

    /**
     * {
     *      "invalid",
     *      "json"
     * }
     */
    @Multiline
    static String invalidJSON;

    @Test
    void invalidJSON() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, invalidJSON)
                .build();
        Message output = jsonParser
                .parse(input);
        assertTrue(output.getError().isPresent(),
                "Expected an error because of the invalid JSON.");
    }

    @Test
    void missingInputField() {
        Message input = Message.builder()
                .build();
        Message output = jsonParser
                .parse(input);
        assertTrue(output.getError().isPresent(),
                "Expected a parsing error because there is no 'input' field to parse.");
    }

    @Test
    void unfoldNested() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, jsonToParse)
                .build();
        Message output = jsonParser
                .normalizer("UNFOLD_NESTED")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("name", "Cake")
                .addField("type", "Donut")
                .addField("ppu", "0.55")
                .addField("batters.batter[0].id", "1001")
                .addField("batters.batter[0].type", "Regular")
                .addField("batters.batter[1].id", "1002")
                .addField("batters.batter[1].type", "Chocolate")
                .addField("topping[0].id", "5001")
                .addField("topping[0].type", "None")
                .addField("topping[1].id", "5002")
                .addField("topping[1].type", "Glazed")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void dropNested() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, jsonToParse)
                .build();
        Message output = jsonParser
                .normalizer("DROP_NESTED")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("name", "Cake")
                .addField("type", "Donut")
                .addField("ppu", "0.55")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void allowNested() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, jsonToParse)
                .build();
        Message output = jsonParser
                .normalizer("ALLOW_NESTED")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("name", "Cake")
                .addField("type", "Donut")
                .addField("ppu", "0.55")
                .addField("batters", "{\"batter\":[{\"id\":\"1001\",\"type\":\"Regular\"},{\"id\":\"1002\",\"type\":\"Chocolate\"}]}")
                .addField("topping", "[{\"id\":\"5001\",\"type\":\"None\"},{\"id\":\"5002\",\"type\":\"Glazed\"}]")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void disallowNested() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, jsonToParse)
                .build();
        Message output = jsonParser
                .normalizer("DISALLOW_NESTED")
                .parse(input);
        assertTrue(output.getError().isPresent(),
                "Expected an error because nested jsonToParse have been disallowed.");
    }

    @Test
    void invalidNormalizer() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, jsonToParse)
                .build();
        assertThrows(IllegalArgumentException.class, () -> jsonParser.normalizer("INVALID").parse(input));
    }
}
