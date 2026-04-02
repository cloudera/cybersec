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
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.JobTester;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
public class TestMessageFileJob extends AbstractParserJobTest {

    @Test
    public void testFailureNoAllowedPaths() {
        ParameterTool params = ParameterTool.fromMap(new HashMap<String, String>() {{
            put(SIGNATURE_ENABLED, "false");
            put(PARAM_CHAIN_CONFIG_FILE, ParserTestUtils.resolveResourcePath("message_file/VpcFlowChain.json"));
            put(PARAM_TOPIC_MAP_CONFIG_FILE, ParserTestUtils.resolveResourcePath("message_file/VpcTopicMap.json"));
        }});

        assertThatThrownBy(() ->setupErrorPipeline(params)).isInstanceOf(RuntimeException.class).hasMessageContaining(ParserJob.PARMA_ALLOWED_MESSAGE_FILE_PATHS).hasMessageContaining("paths that the parser is allowed");
    }

    private void setupErrorPipeline(ParameterTool params) throws Exception {
        try (StreamExecutionEnvironment pipeline = createPipeline(params)) {
            Assertions.fail("Parameters were incorrect.  Test should have failed.");
        }
    }
    @Test
    public void testSuccessfulMessageFile() throws Exception {
        ParameterTool params = ParameterTool.fromMap(new HashMap<String, String>() {{
            put(SIGNATURE_ENABLED, "false");
            put(PARAM_CHAIN_CONFIG_FILE, ParserTestUtils.resolveResourcePath("message_file/VpcFlowChain.json"));
            put(PARAM_TOPIC_MAP_CONFIG_FILE, ParserTestUtils.resolveResourcePath("message_file/VpcTopicMap.json"));
            put(PARMA_ALLOWED_MESSAGE_FILE_PATHS, MessageFileParserTestUtil.getValidMessageFileAllowedPath());
        }});

        StreamExecutionEnvironment env = createPipeline(params);

        JobTester.startTest(env);
        MessageToParse messageToParse = TestUtils.createMessageToParse(ParserTestUtils.resolveResourcePath("message_file/vpc_flow_samples.txt"), "test_topic").build();
        source.sendRecord(messageToParse);
        JobTester.stopTest();
        List<Message> output = new ArrayList<>();

        int expectedRecordCount = 3;
        log.debug("Waiting for {} test records", expectedRecordCount);
        for (int i = 0; i < expectedRecordCount; i++) {
            try {
                Message nextMessage = sink.poll(Duration.ofMillis(100));
                log.debug("got test record {}", nextMessage);
                output.add(nextMessage);
            } catch (TimeoutException e) {
                log.info("Caught timeout exception.", e);
            }
        }

        MessageFileParserTestUtil.verifyMessageFileOutput(output, messageToParse);
    }
}
