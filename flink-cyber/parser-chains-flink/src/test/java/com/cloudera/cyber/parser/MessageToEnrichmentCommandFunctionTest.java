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

import com.cloudera.cyber.*;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentFieldsConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.apache.flink.streaming.util.ProcessFunctionTestHarnesses;
import org.apache.flink.util.OutputTag;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;

import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat.HBASE_METRON;
import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig.DEFAULT_ENRICHMENT_STORAGE_NAME;
import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentFieldsConfig.DEFAULT_KEY_DELIMITER;
import static org.assertj.core.api.Assertions.assertThat;

public class MessageToEnrichmentCommandFunctionTest {

    private static final String TEST_SOURCE = "test_source";
    private static final String TEST_ENRICHMENT_TYPE = "test_enrich_type";
    private static final EnrichmentStorageConfig DEFAULT_STORAGE = new EnrichmentStorageConfig(HBASE_METRON, "enrichments", "cf");
    private static final Map<String, EnrichmentStorageConfig> DEFAULT_STORAGE_CONFIGS = ImmutableMap.of(DEFAULT_ENRICHMENT_STORAGE_NAME, DEFAULT_STORAGE);
    private static final String KEY_1 = "key_1";
    private static final String KEY_VALUE_1 = "k1";
    private static final String KEY_2 = "key_2";
    private static final String KEY_VALUE_2 = "k2";
    private static final String VALUE_1 = "value_1";
    private static final String VALUE_1_VALUE = "v1";
    private static final String VALUE_2 = "value_2";
    private static final String VALUE_2_VALUE = "v2";

    private static final ArrayList<String> KEY_FIELDS = Lists.newArrayList(KEY_1);
    private static final ArrayList<String> KEY_FIELD_VALUES = Lists.newArrayList(KEY_VALUE_1);
    private static final ArrayList<String> MULTI_KEY_FIELDS = Lists.newArrayList(KEY_1, KEY_2);
    private static final ArrayList<String> MULTI_KEY_FIELD_VALUES = Lists.newArrayList(KEY_VALUE_1, KEY_VALUE_2);
    private static final ArrayList<String> VALUE_FIELDS = Lists.newArrayList(VALUE_1);
    private static final Map<String, String> VALUE_FIELD_VALUES = ImmutableMap.of(VALUE_1, VALUE_1_VALUE,
                                                                                  VALUE_2, VALUE_2_VALUE);
    private static final Map<String, String> VALUE_FIELD_SUBSET_VALUES = ImmutableMap.of(VALUE_1, VALUE_1_VALUE);
    private static final ArrayList<String> STREAMING_SOURCES = Lists.newArrayList(TEST_SOURCE);
    private static final OutputTag<Message> ERROR_OUTPUT = new OutputTag<Message>(ParserJob.ERROR_MESSAGE_SIDE_OUTPUT){};

    @Test
    public void testExtractSpecificValueEnrichment() throws Exception {
        Map<String, String> messageExtensions = ImmutableMap.of(KEY_1, KEY_VALUE_1,
                VALUE_1, VALUE_1_VALUE,
                VALUE_2, VALUE_2_VALUE,
                "extra", "ignore");
        testSuccessful(messageExtensions, KEY_FIELDS, null, VALUE_FIELDS,
                KEY_FIELD_VALUES, VALUE_FIELD_VALUES);
        testSuccessful(messageExtensions, KEY_FIELDS, ".", VALUE_FIELDS,
                KEY_FIELD_VALUES, VALUE_FIELD_VALUES);

        messageExtensions = ImmutableMap.of(KEY_1, KEY_VALUE_1,
                VALUE_1, VALUE_1_VALUE,
                "extra", "ignore");
        testSuccessful(messageExtensions, KEY_FIELDS, null, VALUE_FIELDS,
                KEY_FIELD_VALUES, VALUE_FIELD_SUBSET_VALUES);
    }

    @Test
    public void testExtractAllValueEnrichment() throws Exception {
        Map<String, String> messageExtensions = ImmutableMap.of(KEY_1, KEY_VALUE_1,
                VALUE_1, VALUE_1_VALUE,
                "extra", "include");
        Map<String, String> enrichmentValues = messageExtensions.entrySet().stream().filter(e -> e.getKey().equals(KEY_1)).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        testSuccessful(messageExtensions, KEY_FIELDS, null, null,
                KEY_FIELD_VALUES, VALUE_FIELD_VALUES);
        testSuccessful(messageExtensions, KEY_FIELDS, ".", null,
                KEY_FIELD_VALUES, enrichmentValues);
    }

    @Test
    public void testExtractMultiFieldKey() throws Exception {
        Map<String, String> messageExtensions = ImmutableMap.of(KEY_1, KEY_VALUE_1,
                KEY_2, KEY_VALUE_2,
                VALUE_1, VALUE_1_VALUE,
                VALUE_2, VALUE_2_VALUE,
                "extra", "ignore");
        testSuccessful(messageExtensions, MULTI_KEY_FIELDS, null, VALUE_FIELDS,
                MULTI_KEY_FIELD_VALUES, VALUE_FIELD_VALUES);
        testSuccessful(messageExtensions, MULTI_KEY_FIELDS, ".", VALUE_FIELDS,
                MULTI_KEY_FIELD_VALUES, VALUE_FIELD_VALUES);
    }

    @Test
    public void testNullExtensions() throws Exception {
        DataQualityMessage expectedMessageKey1 = DataQualityMessage.builder().
                level(DataQualityMessageLevel.ERROR.name()).
                feature(MessageToEnrichmentCommandFunction.STREAMING_ENRICHMENT_FEATURE).
                field(KEY_1).
                message(String.format(MessageToEnrichmentCommandFunction.STREAMING_ENRICHMENT_KEY_FIELD_NOT_SET, TEST_ENRICHMENT_TYPE)).
                build();
        DataQualityMessage expectedMessageKey2 = DataQualityMessage.builder().
                level(DataQualityMessageLevel.ERROR.name()).
                feature(MessageToEnrichmentCommandFunction.STREAMING_ENRICHMENT_FEATURE).
                field(KEY_2).
                message(String.format(MessageToEnrichmentCommandFunction.STREAMING_ENRICHMENT_KEY_FIELD_NOT_SET, TEST_ENRICHMENT_TYPE)).
                build();
        testError(null,
                Lists.newArrayList(expectedMessageKey1, expectedMessageKey2));
    }

    @Test
    public void testMissingKeyField() throws Exception {

        Map<String, String> messageExtensions = ImmutableMap.of(KEY_1, KEY_VALUE_1,
                VALUE_1, VALUE_1_VALUE,
                VALUE_2, VALUE_2_VALUE,
                "extra", "ignore");

        DataQualityMessage expectedMessage = DataQualityMessage.builder().
                level(DataQualityMessageLevel.ERROR.name()).
                feature(MessageToEnrichmentCommandFunction.STREAMING_ENRICHMENT_FEATURE).
                field(KEY_2).
                message(String.format(MessageToEnrichmentCommandFunction.STREAMING_ENRICHMENT_KEY_FIELD_NOT_SET, TEST_ENRICHMENT_TYPE)).
                build();
        testError(messageExtensions,
                Collections.singletonList(expectedMessage));
    }

    @Test
    public void testNoValues() throws Exception {

        Map<String, String> messageExtensions = ImmutableMap.of(KEY_1, KEY_VALUE_1,
                KEY_2, KEY_VALUE_2,
                "extra", "ignore");

        DataQualityMessage expectedMessage = DataQualityMessage.builder().
                level(DataQualityMessageLevel.ERROR.name()).
                feature(MessageToEnrichmentCommandFunction.STREAMING_ENRICHMENT_FEATURE).
                field(MessageToEnrichmentCommandFunction.VALUE_FIELDS).
                message(String.format(MessageToEnrichmentCommandFunction.STREAMING_ENRICHMENT_VALUE_FIELD_NOT_SET, TEST_ENRICHMENT_TYPE)).
                build();
        testError(messageExtensions,
                Collections.singletonList(expectedMessage));
    }

    private void testError(Map<String, String> messageExtensions,
                           List<DataQualityMessage> expectedDataQuality) throws Exception {

        List<String> streamingEnrichmentSources = Collections.singletonList(TEST_SOURCE);
        EnrichmentsConfig enrichmentsConfig = createEnrichmentsConfig(MessageToEnrichmentCommandFunctionTest.MULTI_KEY_FIELDS, null, MessageToEnrichmentCommandFunctionTest.VALUE_FIELDS);
        OneInputStreamOperatorTestHarness<Message, EnrichmentCommand> testHarness = createTestHarness(streamingEnrichmentSources, enrichmentsConfig);
        Message sentMessage = TestUtils.createMessage(MessageUtils.getCurrentTimestamp(), TEST_SOURCE, messageExtensions);
        testHarness.processElement(new StreamRecord<>(sentMessage));

        Message outputMessage = Objects.requireNonNull(testHarness.getSideOutput(ERROR_OUTPUT).poll()).getValue();
        Message expectedMessage = MessageUtils.enrich(sentMessage, Collections.emptyMap(), expectedDataQuality);
        assertThat(outputMessage).isEqualTo(expectedMessage);
    }

    private void testSuccessful(Map<String, String> messageExtensions,
                                ArrayList<String> keyFields, String delimiter, ArrayList<String> valueFields,
                                List<String> enrichmentKeys, Map<String, String> enrichmentValues) throws Exception {
        Map<String, EnrichmentCommand> expectedResults = new HashMap<>();
        List<String> streamingEnrichmentSources = Collections.singletonList(TEST_SOURCE);
        EnrichmentsConfig enrichmentsConfig = createEnrichmentsConfig(keyFields, delimiter, valueFields);
        OneInputStreamOperatorTestHarness<Message, EnrichmentCommand> testHarness = createTestHarness(streamingEnrichmentSources, enrichmentsConfig);
        sendMessage(expectedResults, testHarness, messageExtensions, createEnrichmentCommand(enrichmentKeys, delimiter, enrichmentValues));
        List<EnrichmentCommand> outputMessages = testHarness.extractOutputValues();

        outputMessages.forEach( enrichCommand -> {{
            EnrichmentCommand expectedCommand = expectedResults.get(enrichCommand.getPayload().getType());
            assertThat(enrichCommand).isEqualTo(expectedCommand);
        }});
    }

    private void sendMessage(Map<String, EnrichmentCommand> expectedEnrichments, OneInputStreamOperatorTestHarness<Message, EnrichmentCommand> harness, Map<String, String> extensions,
                             EnrichmentCommand expectedEnrichmentCommand) throws Exception {
        harness.processElement(new StreamRecord<>(TestUtils.createMessage(MessageUtils.getCurrentTimestamp(), TEST_SOURCE, extensions)));
        expectedEnrichments.put(expectedEnrichmentCommand.getPayload().getType(), expectedEnrichmentCommand);
    }

    private EnrichmentsConfig createEnrichmentsConfig(ArrayList<String> keyFieldsNames, String delimiter, ArrayList<String> valueFieldNames) {
        Map<String, EnrichmentConfig> fieldsConfig = ImmutableMap.of(TEST_ENRICHMENT_TYPE, new EnrichmentConfig(null, new EnrichmentFieldsConfig(keyFieldsNames, delimiter, valueFieldNames, STREAMING_SOURCES)));
        EnrichmentsConfig enrichmentsConfig = new EnrichmentsConfig(DEFAULT_STORAGE_CONFIGS, fieldsConfig);
        enrichmentsConfig.validate();
        return enrichmentsConfig;
    }

    private EnrichmentCommand createEnrichmentCommand(List<String> keyValues, String delimiter, Map<String, String> values) {
        String joiningDelimiter = delimiter != null ? delimiter : DEFAULT_KEY_DELIMITER;
        String enrichmentKey = String.join(joiningDelimiter, keyValues);
        EnrichmentEntry enrichmentEntry = EnrichmentEntry.builder().ts(MessageUtils.getCurrentTimestamp()).
                type(MessageToEnrichmentCommandFunctionTest.TEST_ENRICHMENT_TYPE).
                key(enrichmentKey).entries(values).build();
        return EnrichmentCommand.builder().
                headers(Collections.emptyMap()).
                type(CommandType.ADD).payload(enrichmentEntry).build();
    }

    private OneInputStreamOperatorTestHarness<Message, EnrichmentCommand> createTestHarness(List<String> streamingEnrichmentSources, EnrichmentsConfig streamingEnrichmentConfig) throws Exception {
       return ProcessFunctionTestHarnesses.forProcessFunction(new MessageToEnrichmentCommandFunction(streamingEnrichmentSources, streamingEnrichmentConfig));
    }

}
