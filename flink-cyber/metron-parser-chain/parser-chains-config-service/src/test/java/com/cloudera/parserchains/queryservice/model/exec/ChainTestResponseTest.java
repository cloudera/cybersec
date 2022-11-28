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
