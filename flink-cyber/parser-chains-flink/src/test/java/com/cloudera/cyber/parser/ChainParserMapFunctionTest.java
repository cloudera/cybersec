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

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.parser.wrappers.MessageFileParser;
import com.cloudera.parserchains.core.InvalidParserException;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.google.common.io.Resources;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.apache.flink.util.OutputTag;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.Signature;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.cloudera.cyber.parser.ChainParserMapFunction.CHAIN_PARSER_FEATURE;
import static com.cloudera.cyber.parser.ChainParserMapFunction.NO_TIMESTAMP_FIELD_MESSAGE;
import static com.cloudera.cyber.parser.ChainParserMapFunction.TIMESTAMP_NOT_EPOCH;
import static com.cloudera.parserchains.core.Constants.DEFAULT_ORIGINAL_FILE_LINE_FIELD;
import static com.cloudera.cyber.parser.wrappers.SingleMessageParser.EMPTY_SIGNATURE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("UnstableApiUsage")
public class ChainParserMapFunctionTest {

    private static final String TEST_TOPIC = "test_topic";
    private static final int TEST_PARTITION = 2;
    private static final String TEST_SOURCE = "test_source";
    private static final OutputTag<Message> ERROR_OUTPUT = new OutputTag<Message>(ParserJob.ERROR_MESSAGE_SIDE_OUTPUT){};


    @Test
    public void testTimestampNotNumber() throws Exception {
        testMessageWithError("{\"timestamp\": \"not a number\" }", TIMESTAMP_NOT_EPOCH);
    }

    @Test
    public void testNoTimestamp() throws Exception {
        testMessageWithError("{\"wrong_field_name\": \"1616706642\" }", NO_TIMESTAMP_FIELD_MESSAGE);
    }

    @Test
    public void invalidTopicRegex() {
       assertThatThrownBy(() ->testOpenError("GrokTimestampParserChain.json", "pattern_errors/BadTopicRegex.json")).
                isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Pattern '[' did not compile.");
    }

    @Test
    public void invalidFileRegex() {
        assertThatThrownBy(() ->testOpenError("GrokTimestampParserChain.json", "pattern_errors/BadFileRegex.json")).
                isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Pattern '[' did not compile.");
    }
    @Test
    public void testMessageFile() throws Exception {
        try (OneInputStreamOperatorTestHarness<MessageToParse, Message> harness = createTestHarness("message_file/VpcFlowChain.json", "message_file/VpcTopicMap.json", null)) {
            String filePath = Resources.getResource("message_file/vpc_flow_samples.txt").getFile();
            MessageToParse messageToParse = MessageToParse.builder().offset(1).partition(TEST_PARTITION).topic(TEST_TOPIC).originalBytes(filePath.getBytes(UTF_8)).line(-1).build();
            harness.processElement(new StreamRecord<>(messageToParse));
            List<Message> messages = harness.extractOutputValues();
            MessageFileParserTestUtil.verifyMessageFileOutput(messages, messageToParse);
            assertThat(harness.getSideOutput(ERROR_OUTPUT)).isNull();
        }
    }

    @Test
    public void testMessageFileDoesntExist() throws Exception {
        try (OneInputStreamOperatorTestHarness<MessageToParse, Message> harness = createTestHarness("message_file/VpcFlowChain.json", "message_file/VpcTopicMap.json", null)) {
            harness.processElement(new StreamRecord<>(MessageToParse.builder().offset(1).partition(TEST_PARTITION).topic(TEST_TOPIC).originalBytes("doesnt_exist".getBytes(UTF_8)).line(-1).build()));
            List<Message> messages = harness.extractOutputValues();
            assertThat(messages.isEmpty()).isTrue();
            assertThat(harness.getSideOutput(ERROR_OUTPUT).size()).isEqualTo(1);
        }
    }

    /**
     * Test a combination of file sources and single message sources in the same topic configuration.
     *
     * @throws Exception Exceptions are not expected.
     */
    @Test
    public void testFileAndSingleMessages() throws Exception {
        ParserChainMap chainMap = ParserTestUtils.readParserChainMap("message_file/VpcFlowChain.json");
        chainMap.putAll(ParserTestUtils.readParserChainMap("GrokTimestampParserChain.json"));

        TopicPatternToChainMap topicMap = ParserTestUtils.readTopicMap("message_file/VpcTopicMap.json");
        final String timestampTopic = "single_message_topic";
        final String timestampSource = "timestamp";
        final String vpcLogSource = "vpc_flow_from_file";
        topicMap.put("single_message_topic", new TopicParserConfig("timestamp_log", timestampSource,  null, null));
        try(OneInputStreamOperatorTestHarness<MessageToParse, Message> harness = ProcessFunctionTestHarnesses.forProcessFunction(new ChainParserMapFunction(chainMap, topicMap, null))) {
            String filePath = Resources.getResource("message_file/vpc_flow_samples.txt").getFile();
            harness.processElement(new StreamRecord<>(MessageToParse.builder().offset(1).partition(TEST_PARTITION).topic(TEST_TOPIC).originalBytes(filePath.getBytes(UTF_8)).line(-1).build()));

            long expectedEpochTimestamp = 1616706642L;
            harness.processElement(new StreamRecord<>(MessageToParse.builder().offset(2).partition(TEST_PARTITION).topic(timestampTopic).originalBytes(Long.toString(expectedEpochTimestamp).getBytes(UTF_8)).line(-1).build()));

            List<Message> messages = harness.extractOutputValues();
            assertThat(messages.size()).isEqualTo(4);
            Map<String, Integer> sourceToMessageCount = new HashMap<>();
            for (Message actualMessage : messages) {
                String actualMessageSource = actualMessage.getSource();
                sourceToMessageCount.compute(actualMessageSource, (source, count) -> (count == null) ? 1 : count+1);
                Map<String, String> extensions = actualMessage.getExtensions();
                switch (actualMessageSource) {
                    case timestampSource:
                        assertThat(actualMessage.getTs()).isEqualTo(expectedEpochTimestamp * 1000);
                        break;
                    case vpcLogSource:
                        assertThat(Integer.parseInt(extensions.get(DEFAULT_ORIGINAL_FILE_LINE_FIELD))).isBetween(0, 3);
                        break;
                    case MessageFileParser.MESSAGE_SOURCE_FILE_STATUS:
                        assertThat(extensions.get("filePath")).isEqualTo(filePath);
                        break;
                }
            }
            assertThat(harness.getSideOutput(ERROR_OUTPUT)).isNull();
            assertThat(sourceToMessageCount.get(vpcLogSource)).isEqualTo(2);
            assertThat(sourceToMessageCount.get(timestampSource)).isEqualTo(1);
        }

    }

    @Test
    public void testMessageFiltered() throws Exception {
        try (OneInputStreamOperatorTestHarness<MessageToParse, Message> harness = createTestHarness("metron/parser_chain.json", "metron/topic_map.json", null)) {
            String messageText = ParserTestUtils.readConfigFile("metron/samples/oraclelogon_filtered.txt");
            harness.processElement(new StreamRecord<>(MessageToParse.builder().offset(1).partition(TEST_PARTITION).topic("oraclelogon").originalBytes(messageText.getBytes(UTF_8)).line(-1).build()));
            List<Message> messages = harness.extractOutputValues();
            assertThat(messages.isEmpty()).isTrue();
            assertThat(harness.getSideOutput(ERROR_OUTPUT)).isNull();
        }
    }

    @Test
    public void testMessageEmittedFromFilter() throws Exception {
        try (OneInputStreamOperatorTestHarness<MessageToParse, Message> harness = createTestHarness("metron/parser_chain.json", "metron/topic_map.json", null)) {
            String messageText = ParserTestUtils.readConfigFile("metron/samples/oraclelogon.txt");
            harness.processElement(new StreamRecord<>(MessageToParse.builder().offset(1).partition(TEST_PARTITION).topic("oraclelogon").originalBytes(messageText.getBytes(UTF_8)).line(-1).build()));
            List<Message> messages = harness.extractOutputValues();
            assertThat(messages.size()).isEqualTo(1);
            assertThat(messages.get(0).getExtensions().get("oracle_user")).isEqualTo("SYSMAN");
            assertThat(harness.getSideOutput(ERROR_OUTPUT)).isNull();
        }
    }

    @Test
    public void testExceptionThrownWhenPatternAbsent() {
        assertThatExceptionOfType(InvalidParserException.class).isThrownBy(() -> testOpenError("metron/parser_chain_invalid.json", "metron/topic_map.json"));
    }

    private void testMessageWithError(String messageText, String timestampNotEpoch) throws Exception {
        Message outputMessage;
        try (OneInputStreamOperatorTestHarness<MessageToParse, Message> harness = createTestHarness("JsonTimestampParserChain.json", null)) {
            harness.processElement(new StreamRecord<>(MessageToParse.builder().offset(1).partition(TEST_PARTITION).topic(TEST_TOPIC).originalBytes(messageText.getBytes(UTF_8)).line(-1).build()));
            outputMessage = Objects.requireNonNull(harness.getSideOutput(ERROR_OUTPUT).poll()).getValue();
        }
        assertThat(outputMessage.getExtensions().get("original_string")).isEqualTo(messageText);
        assertThat(outputMessage.getDataQualityMessages()).hasSize(1);
        DataQualityMessage qualityMessage = outputMessage.getDataQualityMessages().get(0);
        assertThat(qualityMessage.getLevel()).isEqualTo(DataQualityMessageLevel.ERROR.name());
        assertThat(qualityMessage.getFeature()).isEqualTo(CHAIN_PARSER_FEATURE);
        assertThat(qualityMessage.getMessage()).contains(timestampNotEpoch);
    }

    @Test
    public void testInvalidParser() {
        assertThatThrownBy(() ->testOpenError("ErrorParserChain.json", "TimestampTopicMap.json")).
                isInstanceOf(InvalidParserException.class).hasMessageContaining("Unable to find parser in catalog");
    }

    @Test
    public void testTimestamps() throws Exception {
        verifyTimestampParsing(ParserTestUtils.loadPrivateKey());
        verifyTimestampParsing(null);
    }

    private void verifyTimestampParsing(PrivateKey privateKey) throws Exception {
        long epochSeconds = 1616706642L;
        long offset = 1;
        Map<Long, Tuple2<Long, byte[]>> expectedTimestamps = new HashMap<>();
        Signature signature = ParserTestUtils.loadSignature(privateKey);

        OneInputStreamOperatorTestHarness<MessageToParse, Message> harness = createTestHarness("GrokTimestampParserChain.json", privateKey);
        sendTimestampMessage(expectedTimestamps, harness, Long.toString(epochSeconds), offset++,epochSeconds * 1000, signature);

        int millis = 123;
        long epochMillis = epochSeconds * 1000 + millis;
        sendTimestampMessage(expectedTimestamps, harness, Long.toString(epochMillis), offset++, epochMillis, signature);
        sendTimestampMessage(expectedTimestamps,harness, String.format("%d.%d", epochSeconds, millis), offset, epochMillis, signature);
        List<Message> outputMessages = harness.extractOutputValues();

        outputMessages.forEach( m -> {{
            Tuple2<Long, byte[]> expectedResults = expectedTimestamps.get(m.getOriginalSource().getOffset());
            assertThat(m.getTs()).isEqualTo(expectedResults.f0);
            assertThat(m.getOriginalSource().getTopic()).isEqualTo(TEST_TOPIC);
            assertThat(m.getOriginalSource().getPartition()).isEqualTo(TEST_PARTITION);
            assertThat(m.getSource()).isEqualTo(TEST_SOURCE);
            assertThat(m.getOriginalSource().getSignature()).isEqualTo(expectedResults.f1);
        }});
    }


    private void sendTimestampMessage(Map<Long, Tuple2<Long, byte[]>> expectedTimestamps, OneInputStreamOperatorTestHarness<MessageToParse, Message> harness, String messageText, long offset, long expectedTimestamp, Signature signature) throws Exception {
        harness.processElement(new StreamRecord<>(MessageToParse.builder().offset(offset).partition(TEST_PARTITION).topic(TEST_TOPIC).originalBytes(messageText.getBytes(UTF_8)).line(-1).build()));
        Tuple2<Long, byte[]> expectedResult = new Tuple2<>(expectedTimestamp, null);
        if (signature != null) {
            signature.update(messageText.getBytes(UTF_8));
            expectedResult.f1 = signature.sign();
        } else {
            expectedResult.f1 = EMPTY_SIGNATURE;
        }
        expectedTimestamps.put(offset, expectedResult);
    }

    private OneInputStreamOperatorTestHarness<MessageToParse, Message> createTestHarness(String chainConfigFile, PrivateKey privateKey) throws Exception {
        return createTestHarness(chainConfigFile, "TimestampTopicMap.json", privateKey);
    }

    private OneInputStreamOperatorTestHarness<MessageToParse, Message> createTestHarness(String chainConfigFile, String topicMapFile, PrivateKey privateKey) throws Exception {
        ParserChainMap chainSchema = ParserTestUtils.readParserChainMap(chainConfigFile);
        TopicPatternToChainMap topicMap = ParserTestUtils.readTopicMap(topicMapFile);

        return ProcessFunctionTestHarnesses.forProcessFunction(new ChainParserMapFunction(chainSchema, topicMap, privateKey));
    }

    private void testOpenError(String chainConfigFile, String topicMapFile) throws Exception {
        String chainConfig = ParserTestUtils.readConfigFile(chainConfigFile);
        String topicConfig = ParserTestUtils.readConfigFile(topicMapFile);
        ParserChainMap chainSchema = JSONUtils.INSTANCE.load(chainConfig, ParserChainMap.class);
        TopicPatternToChainMap topicMap = JSONUtils.INSTANCE.load(topicConfig, TopicPatternToChainMap.class);
        ChainParserMapFunction function = new ChainParserMapFunction(chainSchema, topicMap, null);
        function.open(new Configuration());
    }

}
