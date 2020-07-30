package com.cloudera.parserchains.core;

import com.cloudera.parserchains.core.catalog.ClassIndexParserCatalog;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.model.define.ParserName;
import com.cloudera.parserchains.core.utils.JSONUtils;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DefaultChainBuilderTest {
    static ParserName parserName = ParserName.of("Some Test Parser");
    private DefaultChainBuilder chainBuilder;

    @BeforeEach
    void beforeEach() {
        chainBuilder = new DefaultChainBuilder(new ReflectiveParserBuilder(), new ClassIndexParserCatalog());
    }

    /**
     * {
     *     "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *     "name" : "My Parser Chain",
     *     "parsers" : [ {
     *       "id" : "8673f8f4-a308-4689-822c-0b01477ef378",
     *       "name" : "Test Parser",
     *       "type" : "com.cloudera.parserchains.core.TestParser",
     *       "config" : {
     *         "inputField" : {
     *           "inputField": "original_string"
     *         }
     *       }
     *     }, {
     *       "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *       "name" : "Test Parser",
     *       "type" : "com.cloudera.parserchains.core.TestParser",
     *       "config" : {
     *         "inputField" : [ {
     *           "inputField": "original_string"
     *         }]
     *       }
     *     }]
     * }
     */
    @Multiline
    private String parserChain;

    @Test
    void success() throws InvalidParserException, IOException {
        Message input = Message.builder()
                .createdBy(LinkName.of("original", parserName))
                .addField(FieldName.of("original_string"), FieldValue.of("Homer Simpson, 740 Evergreen Terrace, (939)-555-0113"))
                .build();
        ParserChainSchema schema = JSONUtils.INSTANCE.load(parserChain, ParserChainSchema.class);
        ChainLink head = chainBuilder.build(schema);
        List<Message> results = head.process(input);

        // validate
        assertThat("Expected 2 results; 1 from each parser in the chain.",
                results.size(), is(2));
        assertThat("Expected the message to have been labelled.",
                results.get(0).getCreatedBy().getLinkName(), is("8673f8f4-a308-4689-822c-0b01477ef378"));
        assertThat("Expected the original field to remain.",
                results.get(0).getFields().keySet(), hasItem(FieldName.of("original_string")));
        assertThat("Expected the message to have been labelled correctly.",
                results.get(1).getCreatedBy().getLinkName(), is("3b31e549-340f-47ce-8a71-d702685137f4"));
        assertThat("Expected the original field to remain.",
                results.get(1).getFields().keySet(), hasItem(FieldName.of("original_string")));
    }

    /**
     * {
     *    "id":"1",
     *    "name":"Hello, Chain",
     *    "parsers":[
     *       {
     *          "name":"Route by Name",
     *          "type":"Router",
     *          "id":"96f5f340-5d96-11ea-89de-3b83ec1839cd",
     *          "config":{
     *          },
     *          "routing":{
     *             "routes":[
     *             ]
     *          }
     *       }
     *    ]
     * }
     */
    @Multiline
    private String missingMatchingField;

    @Test
    void error() throws IOException {
        ParserChainSchema schema = JSONUtils.INSTANCE.load(missingMatchingField, ParserChainSchema.class);
        assertThrows(InvalidParserException.class, () -> chainBuilder.build(schema),
                "Expected exception because the router required a matching field to be defined.");
    }

    /**
     * {
     *    "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *    "name" : "My Parser Chain",
     *    "parsers" : [ {
     *      "id" : "123e4567-e89b-12d3-a456-556642440000",
     *      "name" : "Router",
     *      "type" : "Router",
     *      "config" : { },
     *      "routing" : {
     *        "matchingField" : "name",
     *        "routes" : [ {
     *          "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *          "name" : "successRoute",
     *          "matchingValue" : "Ada Lovelace",
     *          "default" : false,
     *          "subchain" : {
     *            "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *            "name" : "Success Chain",
     *            "parsers" : [ {
     *              "id" : "26bf648f-930e-44bf-a4de-bfd34ac16165",
     *              "name" : "Test Parser",
     *              "type" : "com.cloudera.parserchains.core.TestParser",
     *              "config" : {
     *                "inputField" : [ {
     *                  "inputField" : "input"
     *                } ]
     *              }
     *            } ]
     *          }
     *        }, {
     *          "id" : "cdb0729f-a929-4f3c-9cb7-675b57d10a73",
     *          "name" : "defaultRoute",
     *          "matchingValue" : "",
     *          "default" : true,
     *          "subchain" : {
     *            "id" : "cdb0729f-a929-4f3c-9cb7-675b57d10a73",
     *            "name" : "Default Chain",
     *            "parsers" : [ {
     *              "id" : "bdf7d8be-50b1-4998-8b3f-f525d1e95931",
     *              "name" : "Test Parser",
     *              "type" : "com.cloudera.parserchains.core.TestParser",
     *              "config" : {
     *                "inputField" : [ {
     *                  "inputField" : "input"
     *                } ]
     *              }
     *            } ]
     *          }
     *        } ]
     *      }
     *    }, {
     *         "id" : "26bf648f-930e-44bf-a4de-bfd34ac16165",
     *         "name" : "Test Parser",
     *         "type" : "com.cloudera.parserchains.core.TestParser",
     *         "config" : {
     *           "inputField" : [ {
     *               "inputField" : "input"
     *          } ]
     *       }
     *    } ]
     *  }
     */
    @Multiline
    private String parserAfterRouter;

    @Test
    void parserAfterRouter() throws IOException, InvalidParserException {
        Message input = Message.builder()
                .createdBy(LinkName.of("original", parserName))
                .addField(FieldName.of("original_string"), FieldValue.of("Homer Simpson, 740 Evergreen Terrace, (939)-555-0113"))
                .build();
        ParserChainSchema schema = JSONUtils.INSTANCE.load(parserAfterRouter, ParserChainSchema.class);
        ChainLink head = chainBuilder.build(schema);
        List<Message> results = head.process(input);

        // validate
        assertThat("Expected 2 results; 1 from each parser in the chain.",
                results.size(), is(2));
        assertThat("Expected the message to have gone through the default route.",
                results.get(0).getCreatedBy().getLinkName(), is("bdf7d8be-50b1-4998-8b3f-f525d1e95931"));
        assertThat("Expected the original field to remain.",
                results.get(0).getFields().keySet(), hasItem(FieldName.of("original_string")));
        assertThat("Expected the message to have been labelled correctly.",
                results.get(1).getCreatedBy().getLinkName(), is("26bf648f-930e-44bf-a4de-bfd34ac16165"));
        assertThat("Expected the original field to remain.",
                results.get(1).getFields().keySet(), hasItem(FieldName.of("original_string")));
    }
}
