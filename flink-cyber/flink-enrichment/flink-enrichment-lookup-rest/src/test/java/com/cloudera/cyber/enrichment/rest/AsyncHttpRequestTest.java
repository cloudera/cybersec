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

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.TestUtils;
import com.cloudera.cyber.enrichment.rest.impl.MockRestServer;
import com.google.common.collect.Lists;
import lombok.Data;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AsyncHttpRequestTest extends RestRequestTest {

    @BeforeClass
    public static void createMockServices() {
        startMockServer();
    }

    @AfterClass
    public static void stopMockServices() {
        stopMockServer();
    }

    private Message buildMessage(String source, String fieldName, String fieldValue) {
        return TestUtils.createMessage().toBuilder()
                .ts(1)
                .id(UUID.randomUUID().toString())
                .originalSource(TestUtils.source("test", 0, 0))
                .source(source)
                .extensions(new HashMap<String, String>() {{
                    put(fieldName, fieldValue);
                }}).build();
    }

    @Data
    private static class AsyncResult implements ResultFuture<Message> {

        private Collection<Message> collectionResult;
        private Throwable exceptionResult;
        private CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void complete(Collection<Message> collection) {
            this.collectionResult = collection;
            latch.countDown();
        }

        @Override
        public void completeExceptionally(Throwable throwable) {
            this.exceptionResult = throwable;
            latch.countDown();
        }

        public void awaitCompletion() throws InterruptedException {
            latch.await(1, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testSimpleGetRequestWithExactSourceMatch() throws Exception {

        Map<String, Object> expectedExtensions = new HashMap<String, Object>() {{
            put(String.join(".", MockRestServer.ASSET_PREFIX, MockRestServer.ASSET_LOCATION_PROPERTY), MockRestServer.ASSET_LOCATION);

            put(MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.ASSET_ID);
        }};

        testGetAsset(MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.ASSET_ID, expectedExtensions, Collections.emptyList());
    }

    @Test
    public void testSimpleGetRequestWithSourceInList() throws Exception {

        Map<String, Object> expectedExtensions = new HashMap<String, Object>() {{
            put(String.join(".", MockRestServer.ASSET_PREFIX, MockRestServer.ASSET_LOCATION_PROPERTY), MockRestServer.ASSET_LOCATION);

            put(MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.ASSET_ID);
        }};

        String otherMessageSource = "other";
        testGetAsset(Lists.newArrayList(MockRestServer.ASSET_SOURCE, otherMessageSource) , otherMessageSource, MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.ASSET_ID, expectedExtensions, Collections.emptyList());
    }

    @Test
    public void testSimpleGetRequestWithAnySourceMatch() throws Exception {

        Map<String, Object> expectedExtensions = new HashMap<String, Object>() {{
            put(String.join(".", MockRestServer.ASSET_PREFIX, MockRestServer.ASSET_LOCATION_PROPERTY), MockRestServer.ASSET_LOCATION);

            put(MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.ASSET_ID);
        }};

        testGetAsset(Lists.newArrayList(AsyncHttpRequest.ANY_SOURCE_NAME), MockRestServer.ASSET_SOURCE, MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.ASSET_ID, expectedExtensions, Collections.emptyList());
    }

    @Test
    public void testSimpleSourceDoesntMatch() throws Exception {

        Map<String, Object> expectedExtensions = new HashMap<String, Object>() {{
            put(MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.ASSET_ID);
        }};

        testGetAsset(Lists.newArrayList("not an asset source"), MockRestServer.ASSET_SOURCE, MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.ASSET_ID, expectedExtensions, null);
    }

    @Test
    public void testGetError() throws Exception {
        Map<String, Object> expectedExtensions = new HashMap<String, Object>() {{
            put(MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.SERVER_ERROR_ASSET_ID);
        }};

        String mockHostAndPort = mockRestServer.getMockHostAndPort();
        String url = String.format("%s://%s/asset?id=56", MockRestServer.ENCRYPTED_PROTOCOL, mockHostAndPort);
        String exceptionMessage = String.format(RestRequest.REST_REQUEST_HTTP_FAILURE, "HTTP/1.1 503 Service Unavailable");
        List<DataQualityMessage> expectedQualityMessages = Collections.singletonList(DataQualityMessage.builder().
                feature(AsyncHttpRequest.REST_ENRICHMENT_FEATURE).
                field(MockRestServer.ASSET_PREFIX).
                level(DataQualityMessageLevel.ERROR.name()).
                message(String.format(AsyncHttpRequest.REST_REQUEST_FAILED_QUALITY_MESSAGE, url, "null", exceptionMessage)).
                build());

        testGetAsset(MockRestServer.ASSET_ID_FIELD_NAME, MockRestServer.SERVER_ERROR_ASSET_ID, expectedExtensions, expectedQualityMessages);
    }

    @Test
    public void testExactSourceWithUndefinedVariablesInUrlAndEntity() throws Exception {
        String badFieldName = "bad_field_name";
        Map<String, Object> expectedExtensions = new HashMap<String, Object>() {{
            put(badFieldName, MockRestServer.ASSET_ID);
        }};

        String messageText = String.format(RestRequestKey.UNDEFINED_VARIABLE_MESSAGE, MockRestServer.ASSET_ID_FIELD_NAME, RestRequestKey.URL_SOURCE);
        List<DataQualityMessage> expectedQualityMessages = Collections.singletonList(DataQualityMessage.builder().
                feature(AsyncHttpRequest.REST_ENRICHMENT_FEATURE).
                field(MockRestServer.ASSET_PREFIX).
                level(DataQualityMessageLevel.ERROR.name()).
                message(messageText).
                build());

        testGetAsset(badFieldName, MockRestServer.ASSET_ID, expectedExtensions, expectedQualityMessages);
    }

    @Test
    public void testAnySourceUndefinedVariablesInUrlAndEntity() throws Exception {
        // don't report missing variables with ANY match sources
        String badFieldName = "bad_field_name";
        Map<String, Object> expectedExtensions = new HashMap<String, Object>() {{
            put(badFieldName, MockRestServer.ASSET_ID);
        }};

        testGetAsset(Lists.newArrayList(AsyncHttpRequest.ANY_SOURCE_NAME), "myrandomsource", badFieldName, MockRestServer.ASSET_ID, expectedExtensions, null);
    }

    private void testGetAsset(String fieldName, String assetId, Map<String, Object> expectedExtensions, Collection<DataQualityMessage> expectedDataQualityMessages) throws Exception {
        testGetAsset(Lists.newArrayList(MockRestServer.ASSET_SOURCE), MockRestServer.ASSET_SOURCE, fieldName, assetId, expectedExtensions, expectedDataQualityMessages);
    }

    private void testGetAsset(ArrayList<String> enrichmentSources, String messageSource, String fieldName, String assetId, Map<String, Object> expectedExtensions, Collection<DataQualityMessage> expectedDataQualityMessages) throws Exception {
        AsyncHttpRequest  request = new AsyncHttpRequest(mockRestServer.configureGetAssetRequest(MockRestServer.ENCRYPTED_PROTOCOL).sources(enrichmentSources).build());
        request.open(new Configuration());

        AsyncResult result = new AsyncResult();
        Message sourceMessage = buildMessage(messageSource, fieldName, assetId);
        request.asyncInvoke(sourceMessage, result);
        result.awaitCompletion();
        request.close();

        Assert.assertEquals(1, result.getCollectionResult().size());
        Optional<Message> optionalMessage = result.getCollectionResult().stream().findFirst();
        Assert.assertTrue(optionalMessage.isPresent());
        Message resultMessage = optionalMessage.get();

        Assert.assertEquals(expectedExtensions, resultMessage.getExtensions());
        Assert.assertNull(result.getExceptionResult());
        Assert.assertEquals(expectedDataQualityMessages, resultMessage.getDataQualityMessages());
    }

}
