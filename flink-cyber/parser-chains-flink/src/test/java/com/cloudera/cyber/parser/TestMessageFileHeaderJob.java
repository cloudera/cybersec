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
public class TestMessageFileHeaderJob extends AbstractParserJobTest {

    @Test
    public void testBadMessageHeaderConfigFails() {
        ParameterTool params = ParameterTool.fromMap(new HashMap<>() {{
            put(SIGNATURE_ENABLED, "false");
            put(PARAM_CHAIN_CONFIG_FILE, ParserTestUtils.resolveResourcePath("message_file/VpcFlowChain.json"));
            put(PARAM_TOPIC_MAP_CONFIG_FILE, ParserTestUtils.resolveResourcePath("message_file/InvalidVpcTopicMapWithHeaders.json"));
            put(PARAM_ALLOWED_MESSAGE_FILE_PATHS, MessageFileParserTestUtil.getValidMessageFileAllowedPath());
        }});

        assertThatThrownBy(() ->setupErrorPipeline(params)).isInstanceOf(RuntimeException.class).hasMessageContaining(MessageFileHeader.HEADER_LINE_COUNT_OR_PREFIXES_REQUIRED);
    }

    private void setupErrorPipeline(ParameterTool params) throws Exception {
        try (StreamExecutionEnvironment pipeline = createPipeline(params)) {
            Assertions.fail("Parameters were incorrect.  Test should have failed.");
        }
    }

    @Test
    public void testSuccessfulMessageFile() throws Exception {
        ParameterTool params = ParameterTool.fromMap(new HashMap<>() {{
            put(SIGNATURE_ENABLED, "false");
            put(PARAM_CHAIN_CONFIG_FILE, ParserTestUtils.resolveResourcePath("message_file/VpcFlowChain.json"));
            put(PARAM_TOPIC_MAP_CONFIG_FILE, ParserTestUtils.resolveResourcePath("message_file/VpcTopicMapWithHeaders.json"));
            put(PARAM_ALLOWED_MESSAGE_FILE_PATHS, MessageFileParserTestUtil.getValidMessageFileAllowedPath());
        }});

        StreamExecutionEnvironment env = createPipeline(params);

        JobTester.startTest(env);
        MessageToParse messageToParse = TestUtils.createMessageToParse(ParserTestUtils.resolveResourcePath("message_file/vpc_flow_samples_with_header.txt"), "test_topic").build();
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

        MessageFileParserTestUtil.verifyMessageFileOutput(output, messageToParse, 2);
    }
}
