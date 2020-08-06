package com.cloudera.parserchains.core.model.define;

import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.TestParser;
import com.cloudera.parserchains.core.catalog.AnnotationBasedParserInfoBuilder;
import com.cloudera.parserchains.core.catalog.ParserInfo;
import com.cloudera.parserchains.core.catalog.ParserInfoBuilder;
import com.cloudera.parserchains.core.utils.JSONUtils;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;

public class ParserChainSchemaTest {
    private ParserInfoBuilder parserInfoBuilder = new AnnotationBasedParserInfoBuilder();

    /**
     * {
     *   "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *   "name" : "My Parser Chain",
     *   "parsers" : [ {
     *     "id" : "26bf648f-930e-44bf-a4de-bfd34ac16165",
     *     "name" : "Test Parser",
     *     "type" : "com.cloudera.parserchains.core.TestParser",
     *     "config" : {
     *       "inputField" : [ {
     *         "inputField" : "input"
     *       } ]
     *     }
     *   }, {
     *     "id" : "bdf7d8be-50b1-4998-8b3f-f525d1e95931",
     *     "name" : "Test Parser",
     *     "type" : "com.cloudera.parserchains.core.TestParser",
     *     "config" : {
     *       "inputField" : [ {
     *         "inputField" : "input"
     *       } ],
     *       "outputField" : [ {
     *         "outputField" : "output"
     *       } ]
     *     }
     *   } ]
     * }
     */
    @Multiline
    private String chainWithParsersExpectedJSON;

    @Test
    void chainWithParsersToJSON() throws Exception {
        // create a parser
        ParserSchema parserSchema1 = createParser(TestParser.class)
                .setLabel("26bf648f-930e-44bf-a4de-bfd34ac16165")
                .addConfig("inputField",
                        new ConfigValueSchema()
                                .addValue("inputField", "input"));

        // create another parser
        ParserSchema parserSchema2 = createParser(TestParser.class)
                .setLabel("bdf7d8be-50b1-4998-8b3f-f525d1e95931")
                .addConfig("inputField",
                        new ConfigValueSchema()
                                .addValue("inputField", "input"))
                .addConfig("outputField",
                        new ConfigValueSchema()
                                .addValue("outputField", "output"));

        // create the chain
        ParserChainSchema chain = new ParserChainSchema()
                .setId("3b31e549-340f-47ce-8a71-d702685137f4")
                .setName("My Parser Chain")
                .addParser(parserSchema1)
                .addParser(parserSchema2);

        String actual = JSONUtils.INSTANCE.toJSON(chain, true);
        assertThat(actual, equalToCompressingWhiteSpace(chainWithParsersExpectedJSON));
    }

    /**
     * {
     *   "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *   "name" : "My Parser Chain",
     *   "parsers" : [ {
     *     "id" : "26bf648f-930e-44bf-a4de-bfd34ac16165",
     *     "name" : "Test Parser",
     *     "type" : "com.cloudera.parserchains.core.TestParser",
     *     "config" : {
     *       "outputField" : [ {
     *         "inputField" : "input"
     *       } ]
     *     }
     *   }, {
     *     "id" : "123e4567-e89b-12d3-a456-556642440000",
     *     "name" : "Router",
     *     "type" : "Router",
     *     "config" : { },
     *     "routing" : {
     *       "matchingField" : "name",
     *       "routes" : [ {
     *         "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *         "name" : "successRoute",
     *         "matchingValue" : "Ada Lovelace",
     *         "default" : false,
     *         "subchain" : {
     *           "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *           "name" : "Success Chain",
     *           "parsers" : [ {
     *             "id" : "26bf648f-930e-44bf-a4de-bfd34ac16165",
     *             "name" : "Test Parser",
     *             "type" : "com.cloudera.parserchains.core.TestParser",
     *             "config" : {
     *               "inputField" : [ {
     *                 "inputField" : "input"
     *               } ]
     *             }
     *           } ]
     *         }
     *       }, {
     *         "id" : "cdb0729f-a929-4f3c-9cb7-675b57d10a73",
     *         "name" : "defaultRoute",
     *         "matchingValue" : "",
     *         "default" : true,
     *         "subchain" : {
     *           "id" : "cdb0729f-a929-4f3c-9cb7-675b57d10a73",
     *           "name" : "Default Chain",
     *           "parsers" : [ {
     *             "id" : "bdf7d8be-50b1-4998-8b3f-f525d1e95931",
     *             "name" : "Test Parser",
     *             "type" : "com.cloudera.parserchains.core.TestParser",
     *             "config" : {
     *               "inputField" : [ {
     *                 "inputField" : "input"
     *               } ],
     *               "outputField" : [ {
     *                 "outputField" : "output"
     *               } ]
     *             }
     *           } ]
     *         }
     *       } ]
     *     }
     *   } ]
     * }
     */
    @Multiline
    private String chainWithRoutingExpectedJSON;

    @Test
    void chainWithRoutingToJSON() throws Exception {
        // create the "success" route
        ParserSchema successParser = createParser(TestParser.class)
                .setLabel("26bf648f-930e-44bf-a4de-bfd34ac16165")
                .addConfig("inputField", new ConfigValueSchema().addValue("inputField", "input"));
        ParserChainSchema successChain = new ParserChainSchema()
                .setId("3b31e549-340f-47ce-8a71-d702685137f4")
                .setName("Success Chain")
                .addParser(successParser);
        RouteSchema successRoute = new RouteSchema()
                .setLabel("3b31e549-340f-47ce-8a71-d702685137f4")
                .setName(ParserName.of("successRoute"))
                .setDefault(false)
                .setMatchingValue("Ada Lovelace")
                .setSubChain(successChain);

        // create the "default" route
        ParserSchema defaultParser = createParser(TestParser.class)
                .setLabel("bdf7d8be-50b1-4998-8b3f-f525d1e95931")
                .addConfig("inputField", new ConfigValueSchema().addValue("inputField", "input"))
                .addConfig("outputField", new ConfigValueSchema().addValue("outputField", "output"));
        ParserChainSchema defaultChain = new ParserChainSchema()
                .setId("cdb0729f-a929-4f3c-9cb7-675b57d10a73")
                .setName("Default Chain")
                .addParser(defaultParser);
        RouteSchema defaultRoute = new RouteSchema()
                .setLabel("cdb0729f-a929-4f3c-9cb7-675b57d10a73")
                .setName(ParserName.of("defaultRoute"))
                .setDefault(true)
                .setMatchingValue("")
                .setSubChain(defaultChain);

        // define the router
        RoutingSchema routingSchema = new RoutingSchema()
                .setMatchingField("name")
                .addRoute(successRoute)
                .addRoute(defaultRoute);
        ParserSchema routerSchema = new ParserSchema()
                .setId(ParserID.router())
                .setLabel("123e4567-e89b-12d3-a456-556642440000")
                .setName(ParserName.router())
                .setRouting(routingSchema);

        // create the main chain
        ParserSchema firstParser = createParser(TestParser.class)
                .setLabel("26bf648f-930e-44bf-a4de-bfd34ac16165")
                .addConfig("outputField", new ConfigValueSchema().addValue("inputField", "input"));
        ParserChainSchema mainChain = new ParserChainSchema()
                .setId("3b31e549-340f-47ce-8a71-d702685137f4")
                .setName("My Parser Chain")
                .addParser(firstParser)
                .addParser(routerSchema);

        String actual = JSONUtils.INSTANCE.toJSON(mainChain, true);
        assertThat(actual, equalToCompressingWhiteSpace(chainWithRoutingExpectedJSON));
    }

    private ParserSchema createParser(Class<? extends Parser> parserClass) {
        ParserInfo parserInfo = parserInfoBuilder.build(parserClass).get();
        return new ParserSchema()
                .setId(ParserID.of(parserClass))
                .setName(ParserName.of(parserInfo.getName()));
    }
}
