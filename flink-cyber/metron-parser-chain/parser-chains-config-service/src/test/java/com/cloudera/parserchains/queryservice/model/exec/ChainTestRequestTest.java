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

import com.cloudera.parserchains.core.catalog.AnnotationBasedParserInfoBuilder;
import com.cloudera.parserchains.core.catalog.ParserInfo;
import com.cloudera.parserchains.core.catalog.ParserInfoBuilder;
import com.cloudera.parserchains.core.model.define.ConfigValueSchema;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.model.define.ParserName;
import com.cloudera.parserchains.core.model.define.ParserSchema;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.parsers.DelimitedTextParser;
import com.cloudera.parserchains.parsers.TimestampParser;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummaryMapper;
import com.cyber.jackson.core.JsonProcessingException;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;

public class ChainTestRequestTest {
    private ParserInfoBuilder parserInfoBuilder = new AnnotationBasedParserInfoBuilder();

    /**
     * {
     *   "sampleData" : {
     *     "type" : "manual",
     *     "source" : [
     *       "Marie, Curie"
     *      ]
     *   },
     *   "chainConfig" : {
     *     "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *     "name" : "My Parser Chain",
     *     "parsers" : [ {
     *       "id" : "3b31e549-340f-47ce-8a71-d702685137f4",
     *       "name" : "Delimited Text",
     *       "type" : "com.cloudera.parserchains.parsers.DelimitedTextParser",
     *       "config" : {
     *         "outputField" : [ {
     *           "fieldIndex" : "0",
     *           "fieldName" : "firstName"
     *         }, {
     *           "fieldIndex" : "1",
     *           "fieldName" : "lastName"
     *         } ]
     *       }
     *     }, {
     *       "id" : "74d10881-ae37-4c90-95f5-ae0c10aae1f4",
     *       "name" : "Timestamp",
     *       "type" : "com.cloudera.parserchains.parsers.TimestampParser",
     *       "config" : {
     *         "outputField" : [ {
     *           "outputField" : "timestamp"
     *         } ]
     *       }
     *     } ]
     *   }
     * }
     */
    @Multiline
    private String expected;

    @Test
    void toJSON() throws JsonProcessingException {
        ChainTestRequest request = new ChainTestRequest()
                .setParserChainSchema(parserChainSchema())
                .setSampleData(new SampleData()
                        .addSource("Marie, Curie")
                        .setType("manual")
                );
        String actual = JSONUtils.INSTANCE.toJSON(request, true);
        assertThat(actual, equalToCompressingWhiteSpace(expected));
    }

    private ParserChainSchema parserChainSchema() {
        return new ParserChainSchema()
                    .setId("3b31e549-340f-47ce-8a71-d702685137f4")
                    .setName("My Parser Chain")
                    .addParser(csvParserSchema())
                    .addParser(timestampParserSchema());
    }

    private ParserSchema timestampParserSchema() {
        ParserInfo timestampInfo = parserInfoBuilder.build(TimestampParser.class).get();
        ParserSummary timestampType = new ParserSummaryMapper()
                .reform(timestampInfo);
        return new ParserSchema()
                .setId(timestampType.getId())
                .setLabel("74d10881-ae37-4c90-95f5-ae0c10aae1f4")
                .setName(ParserName.of(timestampInfo.getName()))
                .addConfig("outputField",
                        new ConfigValueSchema()
                                .addValue("outputField", "timestamp")
                );
    }

    private ParserSchema csvParserSchema() {
        ParserInfo csvInfo = parserInfoBuilder.build(DelimitedTextParser.class).get();
        ParserSummary csvType = new ParserSummaryMapper()
                .reform(csvInfo);
        return new ParserSchema()
                .setId(csvType.getId())
                .setLabel("3b31e549-340f-47ce-8a71-d702685137f4")
                .setName(ParserName.of(csvInfo.getName()))
                .addConfig("outputField",
                        new ConfigValueSchema()
                                .addValue("fieldName", "firstName")
                                .addValue("fieldIndex", "0")
                )
                .addConfig("outputField",
                        new ConfigValueSchema()
                                .addValue("fieldName", "lastName")
                                .addValue("fieldIndex", "1")
                );
    }
}
