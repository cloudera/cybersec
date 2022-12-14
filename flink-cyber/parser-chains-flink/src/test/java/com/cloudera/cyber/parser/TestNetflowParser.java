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

package com.cloudera.cyber.parser;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.google.common.io.Resources;
import org.adrianwalker.multilinestring.Multiline;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class TestNetflowParser extends AbstractParserJobTest {

    /**
     * {
     * "netflow": {
     * "id": "220ee8c5-07d7-48d9-8df5-7d23376cb664",
     * "name": "Netflow Parser",
     * "parsers": [
     * {
     * "id": "f812c6dc-40cc-4c77-abf8-e15fccdfea32",
     * "name": "Netflow as JSON",
     * "type": "com.cloudera.parserchains.parsers.JSONParser",
     * "config": {
     * "input" : { "input": "original_string" },
     * "norm": { "norm": "UNFOLD_NESTED" }
     * }
     * },
     * {
     * "id": "6b8797a2-95df-4021-83c2-60ac4c786e67",
     * "name": "Field Renamer",
     * "type" : "com.cloudera.parserchains.parsers.RenameFieldParser",
     * "config": {
     * "fieldToRename": [
     * { "from": "@timestamp", "to":"timestamp" }
     * ]
     * }
     * },
     * {
     * "id": "9549004f-83e4-4d24-8baa-abdbdad06e61",
     * "name": "Timestamp Parser",
     * "type" : "com.cloudera.parserchains.parsers.TimestampFormatParser",
     * "config": {
     * "fields": [
     * { "field": "timestamp", "format":"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "tz": "" }
     * ]
     * }
     * }
     * ]
     * }
     * }
     */
    @Multiline
    private String config;


    @Test
    public void testParser() throws Exception {
        ParameterTool params = ParameterTool.fromMap(new HashMap<String, String>() {{
            put(PARAM_CHAIN_CONFIG, config);
            put(PARAM_PRIVATE_KEY, getKeyBase64());
        }});

        StreamExecutionEnvironment env = createPipeline(params);

        JobTester.startTest(env);

        source.sendRecord(TestUtils
                .createMessageToParse(
                        Resources.toString(Resources.getResource("netflow.json"), StandardCharsets.UTF_8)
                )
                .topic("netflow")
                .build());
        JobTester.stopTest();

        Message out = sink.poll(Duration.ofMillis(1000));
        assertThat("Output not null", out, notNullValue());
        assertThat("Original String is moved", out.getExtensions(), not(hasKey(DEFAULT_INPUT_FIELD)));
        assertThat("Timestamp is moved", out.getExtensions(), not(hasKey("timestamp")));
    }
}
