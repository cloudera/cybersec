package com.cloudera.parserchains.queryservice.model.exec;

import com.cloudera.parserchains.core.utils.JSONUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;

public class ChainTestResponseTest {

    /**
     * {
     *   "results" : [ {
     *     "input" : {
     *       "original_string" : "foo, bar, baz"
     *     },
     *     "output" : {
     *       "original_string" : "foo, bar, baz",
     *       "timestamp" : "1584721517455"
     *     },
     *     "log" : {
     *       "type" : "info",
     *       "message" : "success",
     *       "parserId" : "74d10881-ae37-4c90-95f5-ae0c10aae1f4"
     *     },
     *     "parserResults" : [ {
     *       "input" : {
     *         "original_string" : "foo, bar, baz"
     *       },
     *       "output" : {
     *         "original_string" : "foo, bar, baz",
     *         "timestamp" : "1584721517458"
     *       },
     *       "log" : {
     *         "type" : "info",
     *         "message" : "success",
     *         "parserId" : "d8f354dd-51b8-4faf-a7a2-39f73a9bc3dc"
     *       }
     *     } ]
     *   }, {
     *     "input" : {
     *       "original_string" : "foo, bar, baz"
     *     },
     *     "output" : {
     *       "original_string" : "foo, bar, baz",
     *       "timestamp" : "1584721517458"
     *     },
     *     "log" : {
     *       "type" : "info",
     *       "message" : "success",
     *       "parserId" : "3b31e549-340f-47ce-8a71-d702685137f4"
     *     },
     *     "parserResults" : [ {
     *       "input" : {
     *         "original_string" : "foo, bar, baz"
     *       },
     *       "output" : {
     *         "original_string" : "foo, bar, baz",
     *         "timestamp" : "1584721517458"
     *       },
     *       "log" : {
     *         "type" : "info",
     *         "message" : "success",
     *         "parserId" : "bbaa194e-cb1e-41bf-bd3c-c746d13d2acd"
     *       }
     *     } ]
     *   } ]
     * }
     */
    @Multiline
    private String expected;

    @Test
    void multipleMessages() throws JsonProcessingException {
        ChainTestResponse response = new ChainTestResponse()
                // a result for the first message that was parsed
                .addResult(new ParserResult()
                        .addInput("original_string", "foo, bar, baz")
                        .addOutput("original_string", "foo, bar, baz")
                        .addOutput("timestamp", "1584721517455")
                        .setLog(new ResultLog()
                                .setMessage("success")
                                .setParserId("74d10881-ae37-4c90-95f5-ae0c10aae1f4")
                                .setType("info")
                        )
                        // a result for the first parser in the chain
                        .addParserResult(new ParserResult()
                                .addInput("original_string", "foo, bar, baz")
                                .addOutput("original_string", "foo, bar, baz")
                                .addOutput("timestamp", "1584721517458")
                                .setLog(new ResultLog()
                                        .setMessage("success")
                                        .setParserId("d8f354dd-51b8-4faf-a7a2-39f73a9bc3dc")
                                        .setType("info")
                                )
                        )
                )
                // a result for the second message that was parsed
                .addResult(new ParserResult()
                        .addInput("original_string", "foo, bar, baz")
                        .addOutput("original_string", "foo, bar, baz")
                        .addOutput("timestamp", "1584721517458")
                        .setLog(new ResultLog()
                                .setMessage("success")
                                .setParserId("3b31e549-340f-47ce-8a71-d702685137f4")
                                .setType("info")
                        )
                        // a result for the first parser in the chain
                        .addParserResult(new ParserResult()
                                .addInput("original_string", "foo, bar, baz")
                                .addOutput("original_string", "foo, bar, baz")
                                .addOutput("timestamp", "1584721517458")
                                .setLog(new ResultLog()
                                        .setMessage("success")
                                        .setParserId("bbaa194e-cb1e-41bf-bd3c-c746d13d2acd")
                                        .setType("info")
                                )
                        )
                );

        String actual = JSONUtils.INSTANCE.toJSON(response, true);
        assertThat(actual, equalToCompressingWhiteSpace(expected));
    }
}
