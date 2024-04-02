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

package com.cloudera.cyber.enrichment.rest;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.enrichment.ConfigUtils;
import com.cloudera.cyber.enrichment.rest.impl.MockRestServer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.test.util.CollectingSink;
import org.apache.flink.test.util.JobTester;
import org.apache.flink.test.util.ManualSource;
import org.hamcrest.Matchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

@Slf4j
public class RestLookupJobTest extends RestLookupJob {
    private final CollectingSink<Message> sink = new CollectingSink<>();
    private final List<Message> recordLog = new ArrayList<>();
    private final Map<Long, Map<String, String>> expectedExtensions = new HashMap<>();
    private ManualSource<Message> source;
    private static MockRestServer mockRestServer;

    @ClassRule
    public static TemporaryFolder configTempFolder = new TemporaryFolder();
    private static String configFilePath;

    @BeforeClass
    public static void startMockRestServer() throws IOException {
        mockRestServer = new MockRestServer(true);
        File configFile = configTempFolder.newFile("rest-job-test.json");
        List<RestEnrichmentConfig> modelRestConfig = new ArrayList<>();
        modelRestConfig.add(mockRestServer.configureModelPostRequest(MockRestServer.ENCRYPTED_PROTOCOL).build());
        modelRestConfig.add(mockRestServer.configureGetAssetRequest(MockRestServer.ENCRYPTED_PROTOCOL).build());
        modelRestConfig.add(mockRestServer.configureGetUserRequest(MockRestServer.ENCRYPTED_PROTOCOL).build());
        ObjectWriter ow = getConfigObjectMapper().writerFor(new TypeReference<ArrayList<RestEnrichmentConfig>>() {
        });
        ow.writeValue(configFile, modelRestConfig);
        configFilePath = configFile.getPath();
    }

    @AfterClass
    public static void stopMockRestServer() {
        mockRestServer.close();
    }

    @Test
    public void testRestServicePipeline() throws Exception {
        long ts = 0;

        JobTester.startTest(createPipeline(ParameterTool.fromMap(new HashMap<String, String>() {{
            put(ConfigUtils.PARAMS_CONFIG_FILE, configFilePath);
        }})).setParallelism(1));

        createMessages(ts);

        JobTester.stopTest();

        List<Message> output = new ArrayList<>();

        int recordCount = recordLog.size();
        log.debug("Waiting for {} test records", recordCount);
        for (int i = 0; i < recordCount; i++) {
            try {
                Message nextMessage = sink.poll(Duration.ofMillis(100));
                log.debug("got test record {}", nextMessage);
                output.add(nextMessage);
            } catch (TimeoutException e) {
                log.info("Caught timeout exception.", e);
            }
        }

        checkResults(output);
        assertThat("Result count", output, hasSize(recordLog.size()));
    }

    private void createMessages(long ts) {

        long offset = 100;
        long nextTimestamp = ts + offset;

        // create dga model post events
        for(MockRestServer.ExpectedModelResult expectedModelResult : MockRestServer.expectedModelResults) {
            Map<String, String> inputFields = new HashMap<String, String>() {{
                put(MockRestServer.DOMAIN_EXTENSION_NAME, expectedModelResult.getDomainName());
            }};
            nextTimestamp = createMessage(nextTimestamp, MockRestServer.DNS_SOURCE, inputFields, expectedModelResult.getExpectedExtensions(MockRestServer.DGA_MODEL_PREFIX));
        }

        // create an asset get event
        Map<String, String> inputFields = new HashMap<String, String>() {{
            put(MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.ASSET_ID);
        }};
        Map<String, String> expectedExtensionResults = new HashMap<>(inputFields);
        expectedExtensionResults.put(String.join(".", MockRestServer.ASSET_PREFIX, MockRestServer.ASSET_LOCATION_PROPERTY), MockRestServer.ASSET_LOCATION);
        nextTimestamp = createMessage(nextTimestamp, MockRestServer.ASSET_SOURCE, inputFields, expectedExtensionResults);

        // create an invalid asset
        inputFields = new HashMap<String, String>() {{
            put(MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.SERVER_ERROR_ASSET_ID);
        }};
        expectedExtensionResults = new HashMap<>(inputFields);
        nextTimestamp = createMessage(nextTimestamp, MockRestServer.ASSET_SOURCE, inputFields, expectedExtensionResults);

        // create a valid user get request
        inputFields = new HashMap<String, String>() {{
            put(MockRestServer.USER_NAME_PROPERTY, MockRestServer.USER_NAME);
        }};
        expectedExtensionResults = new HashMap<>(inputFields);
        expectedExtensionResults.put(String.join(".", MockRestServer.USER_PREFIX, MockRestServer.DEPARTMENT_NAME_PROPERTY), MockRestServer.DEPARTMENT_NAME);
        nextTimestamp = createMessage(nextTimestamp, MockRestServer.USER_SOURCE, inputFields, expectedExtensionResults);


        source.sendWatermark(nextTimestamp);
        source.markFinished();
    }

    private long createMessage(long nextTimestamp, String source, Map<String, String> inputFields, Map<String, String> expectedExtensionResults) {
        expectedExtensions.put(nextTimestamp, expectedExtensionResults);
        log.debug("adding timestamp {} {}", nextTimestamp, expectedExtensions);
        sendRecord(TestUtils.createMessage().toBuilder()
                .source(source)
                .extensions(inputFields)
                .ts(nextTimestamp));
        return nextTimestamp + 100L;
    }

    private void sendRecord(Message.MessageBuilder d) {
        Message r = d.id(UUID.randomUUID().toString())
                .originalSource(TestUtils.source("test", 0, 0))
                .build();
        this.source.sendRecord(r, r.getTs());
        this.recordLog.add(r);
    }

    private void checkResults(List<Message> results) {

        for (Message result : results) {
            assertThat(String.format("extension fields match - index %d", result.getTs()), result.getExtensions(), Matchers.equalTo(expectedExtensions.get(result.getTs())));
        }
        assertThat("expected number of results returned", expectedExtensions.size(), Matchers.equalTo(results.size()));
    }


    @Override
    protected DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        source = JobTester.createManualSource(env, TypeInformation.of(Message.class));

        return source.getDataStream();
    }

    @Override
    protected void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results) {
        results.addSink(sink);
    }
}



