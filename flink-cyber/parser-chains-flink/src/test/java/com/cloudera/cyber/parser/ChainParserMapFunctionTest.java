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
import com.cloudera.parserchains.core.InvalidParserException;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.google.common.io.Resources;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.apache.flink.util.OutputTag;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.cloudera.cyber.parser.ChainParserMapFunction.CHAIN_PARSER_FEATURE;
import static com.cloudera.cyber.parser.ChainParserMapFunction.EMPTY_SIGNATURE;
import static com.cloudera.cyber.parser.ChainParserMapFunction.NO_TIMESTAMP_FIELD_MESSAGE;
import static com.cloudera.cyber.parser.ChainParserMapFunction.TIMESTAMP_NOT_EPOCH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    public void testMessageFiltered() throws Exception {
        OneInputStreamOperatorTestHarness<MessageToParse, Message> harness = createTestHarness("metron/parser_chain.json", "metron/topic_map.json", null);
        String messageText = readConfigFile("metron/samples/oraclelogon_filtered.txt");
        harness.processElement(new StreamRecord<>(MessageToParse.builder().offset(1).partition(TEST_PARTITION).topic("oraclelogon").originalBytes(messageText.getBytes(UTF_8)).build()));
        List<Message> messages = harness.extractOutputValues();
        assertThat(messages.isEmpty()).isTrue();
        assertThat(harness.getSideOutput(ERROR_OUTPUT)).isNull();
    }

    @Test
    public void testMessageEmittedFromFilter() throws Exception {
        OneInputStreamOperatorTestHarness<MessageToParse, Message> harness = createTestHarness("metron/parser_chain.json", "metron/topic_map.json", null);
        String messageText = readConfigFile("metron/samples/oraclelogon.txt");
        harness.processElement(new StreamRecord<>(MessageToParse.builder().offset(1).partition(TEST_PARTITION).topic("oraclelogon").originalBytes(messageText.getBytes(UTF_8)).build()));
        List<Message> messages = harness.extractOutputValues();
        assertThat(messages.size()).isEqualTo(1);
        assertThat(messages.get(0).getExtensions().get("oracle_user")).isEqualTo("SYSMAN");
        assertThat(harness.getSideOutput(ERROR_OUTPUT)).isNull();
    }

    @Test
    public void testExceptionThrownWhenPatternAbsent() {
        assertThatThrownBy(() -> createTestHarness("metron/parser_chain_invalid.json", "metron/topic_map.json", null))
                .isInstanceOf(InvalidParserException.class);
    }

    private void testMessageWithError(String messageText, String timestampNotEpoch) throws Exception {
        OneInputStreamOperatorTestHarness<MessageToParse, Message> harness = createTestHarness("JsonTimestampParserChain.json", null);
        harness.processElement(new StreamRecord<>(MessageToParse.builder().offset(1).partition(TEST_PARTITION).topic(TEST_TOPIC).originalBytes(messageText.getBytes(UTF_8)).build()));
        Message outputMessage = Objects.requireNonNull(harness.getSideOutput(ERROR_OUTPUT).poll()).getValue();
        assertThat(outputMessage.getExtensions().get("original_string")).isEqualTo(messageText);
        assertThat(outputMessage.getDataQualityMessages()).hasSize(1);
        DataQualityMessage qualityMessage = outputMessage.getDataQualityMessages().get(0);
        assertThat(qualityMessage.getLevel()).isEqualTo(DataQualityMessageLevel.ERROR.name());
        assertThat(qualityMessage.getFeature()).isEqualTo(CHAIN_PARSER_FEATURE);
        assertThat(qualityMessage.getMessage()).contains(timestampNotEpoch);
    }

    @Test
    public void testInvalidParser() {
        assertThatThrownBy(() ->createTestHarness("ErrorParserChain.json", null)).
                isInstanceOf(InvalidParserException.class).hasMessageContaining("Unable to find parser in catalog");
    }

    @Test
    public void testTimestamps() throws Exception {
        PrivateKey privateKey = loadPrivateKey();
        verifyTimestampParsing(privateKey);
        verifyTimestampParsing(null);

    }

    private void verifyTimestampParsing(PrivateKey privateKey) throws Exception {
        long epochSeconds = 1616706642L;
        long offset = 1;
        Map<Long, Tuple2<Long, byte[]>> expectedTimestamps = new HashMap<>();
        Signature signature = loadSignature(privateKey);

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
        harness.processElement(new StreamRecord<>(MessageToParse.builder().offset(offset).partition(TEST_PARTITION).topic(TEST_TOPIC).originalBytes(messageText.getBytes(UTF_8)).build()));
        Tuple2<Long, byte[]> expectedResult = new Tuple2<>(expectedTimestamp, null);
        if (signature != null) {
            signature.update(messageText.getBytes(UTF_8));
            expectedResult.f1 = signature.sign();
        } else {
            expectedResult.f1 = EMPTY_SIGNATURE;
        }
        expectedTimestamps.put(offset, expectedResult);
    }

    private String readConfigFile(String name) throws IOException {
        URL url = Resources.getResource(name);
        return Resources.toString(url, UTF_8);
    }

    private Signature loadSignature(PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException {
        if (privateKey != null) {
            Signature signature = Signature.getInstance("SHA1WithRSA");
            signature.initSign(privateKey);
            return signature;
        } else {
            return null;
        }
    }

    private PrivateKey loadPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        URL url = Resources.getResource("private_key.der");
        byte[] privKeyBytes = Resources.toByteArray(url);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privKeyBytes);
       return keyFactory.generatePrivate(privSpec);
    }

    private OneInputStreamOperatorTestHarness<MessageToParse, Message> createTestHarness(String chainConfigFile, PrivateKey privateKey) throws Exception {
        return createTestHarness(chainConfigFile, "TimestampTopicMap.json", privateKey);
    }

    private OneInputStreamOperatorTestHarness<MessageToParse, Message> createTestHarness(String chainConfigFile, String topicMapFile, PrivateKey privateKey) throws Exception {
        String chainConfig = readConfigFile(chainConfigFile);
        String topicConfig = readConfigFile(topicMapFile);
        ParserChainMap chainSchema = JSONUtils.INSTANCE.load(chainConfig, ParserChainMap.class);
        TopicPatternToChainMap topicMap = JSONUtils.INSTANCE.load(topicConfig, TopicPatternToChainMap.class);

        return ProcessFunctionTestHarnesses.forProcessFunction(new ChainParserMapFunction(chainSchema, topicMap, privateKey, "default"));
    }

}
