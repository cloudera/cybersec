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

import com.cloudera.cyber.enrichment.rest.impl.MockRestServer;
import com.google.common.collect.Lists;
import org.apache.http.client.methods.HttpPost;
import org.hamcrest.Matcher;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class PostRestRequestTest extends RestRequestTest {
    private static RestEnrichmentConfig modelResultPostRequest;

    @BeforeClass
    public static void createMockServices() {
        startMockServer();
        modelResultPostRequest = mockRestServer.configureModelPostRequest(MockRestServer.ENCRYPTED_PROTOCOL).build();
    }

    @AfterClass
    public static void stopMockServices() {
        stopMockServer();
    }

    @Test
    public void testSimplePostCModel() throws Exception {
        testSuccessfulModelPosts(modelResultPostRequest);
    }

    private void testSuccessfulModelPosts(RestEnrichmentConfig config) throws Exception {
        for(MockRestServer.ExpectedModelResult expectedModelResult : MockRestServer.expectedModelResults) {
            if (expectedModelResult.isSuccess()) {
                testDomain(config, expectedModelResult.getDomainName(), expectedModelResult.isLegit());
            }
        }
    }

    @Test
    public void testHttp200StatusButResultIndicatesError() throws Exception {
        String expectedErrorFormat = "Rest request url='%s://%s/model' entity='{\"accessKey\":\"mup8kz1hsl3erczwepbt8jupamita6y6\",\"request\":{\"domain\":\"unsuccessfuldomain\"}}' failed 'Rest request returned a success code but the content indicated failure.'";
        testHttpFailedRequest(MockRestServer.UNSUCCESSFUL_DOMAIN, expectedErrorFormat, modelResultPostRequest, mockRestServer);
    }

    @Test
    public void testHttp400Exception() throws Exception {
        String expectedErrorFormat = "Rest request url='%s://%s/model' entity='{\"accessKey\":\"mup8kz1hsl3erczwepbt8jupamita6y6\",\"request\":{\"domain\":\"clienterror\"}}' failed 'Rest request failed due to 'HTTP/1.1 400 Bad Request'.'";
        testHttpFailedRequest(MockRestServer.CLIENT_ERROR_DOMAIN, expectedErrorFormat, modelResultPostRequest, mockRestServer);
    }

    private void testHttpFailedRequest(String domainName, String errorMessageFormat, RestEnrichmentConfig modelResultPostRequest, MockRestServer mockRestServer) throws Exception {
        RestRequest request = modelResultPostRequest.createRestEnrichmentRequest();
        Map<String, String> variables = new HashMap<String, String>() {
            {
                put(MockRestServer.DOMAIN_EXTENSION_NAME, domainName);
            }
        };
        RestRequestResult result = request.getResult(true, variables).get();
        verifyErrorResult(result, errorMessageFormat, mockRestServer);
    }

    private static class MockThrowingPostRequest extends PostRestRequest {

        public MockThrowingPostRequest(RestEnrichmentConfig config) throws Exception {
            super(config);
        }

        protected void addEntityToRequest(@Nonnull HttpPost postRequest, @Nonnull String entityTemplate) throws UnsupportedEncodingException {
            throw new UnsupportedEncodingException("Default UnsupportedEndcodingException message");
        }
    }

    @Test
    public void testUnsupportedCharacterSetException() throws Exception {
        MockThrowingPostRequest throwingPostRequest = new MockThrowingPostRequest(modelResultPostRequest);
        RestRequestKey key = throwingPostRequest.getKey(new HashMap<String, String>() {{ put(MockRestServer.DOMAIN_EXTENSION_NAME, "mydomain");}});
        CompletableFuture<RestRequestResult> future = new MockThrowingPostRequest(modelResultPostRequest).asyncLoad(key, Executors.newFixedThreadPool(1));
        RestRequestResult result = future.get();
        String expectedErrorFormat = "Rest request url='%s://%s/model' entity='{\"accessKey\":\"mup8kz1hsl3erczwepbt8jupamita6y6\",\"request\":{\"domain\":\"mydomain\"}}' failed 'Default UnsupportedEndcodingException message'";
        verifyErrorResult(result, expectedErrorFormat, mockRestServer);
    }

    @Test
    public void testUndefinedVariableWithAnyMatchIgnoresKeyErrors() throws Exception {
        RestEnrichmentConfig anyMatchRequest = mockRestServer.configureModelPostRequest(MockRestServer.ENCRYPTED_PROTOCOL).sources(Lists.newArrayList(AsyncHttpRequest.ANY_SOURCE_NAME)).build();
        // accessing an undefined field in a key with any source match does not report an error
        testDomain(anyMatchRequest, new ArrayList<>());
    }

    @Test
    public void testTlsWithAlias() throws Exception {
        RestEnrichmentConfig.RestEnrichmentConfigBuilder modelResultPostRequestWithKeyAlias = mockRestServer.configureTLS(mockRestServer.configureModelPostRequest(MockRestServer.ENCRYPTED_PROTOCOL), "client");
        testSuccessfulModelPosts(modelResultPostRequestWithKeyAlias.build());
    }

    @Test
    public void testTlsHandshakeException() throws Exception {
        RestEnrichmentConfig.RestEnrichmentConfigBuilder modelResultPostRequestWithKeyAlias = mockRestServer.configureTLS(mockRestServer.configureModelPostRequest(MockRestServer.ENCRYPTED_PROTOCOL), "nosuchkeyalias");
        RestRequest badHandshakePost = new PostRestRequest(modelResultPostRequestWithKeyAlias.build());
        MockRestServer.ExpectedModelResult expectedResult = MockRestServer.expectedModelResults.get(0);
        Map<String, String> extensions = new HashMap<String, String>() {{
            put(MockRestServer.DOMAIN_EXTENSION_NAME, expectedResult.getDomainName());
        }};
        RestRequestResult result = badHandshakePost.getResult(true, extensions).get();

        Assert.assertTrue(result.getExtensions().isEmpty());
        List<String> errors = result.getErrors();
        Assert.assertEquals(1, errors.size());
        Matcher<String> expectedString = containsString(String.format("Rest request url='%s://%s/model' entity='{\"accessKey\":\"mup8kz1hsl3erczwepbt8jupamita6y6\",\"request\":{\"domain\":\"google\"}}'", mockRestServer.getMockProtocol(), mockRestServer.getMockHostAndPort()));
        assertThat(errors.get(0), expectedString);
    }

    @Test
    public void testUndefinedVariableWithSpecificMatchReturnsKeyErrors() throws Exception {
        RestEnrichmentConfig anyMatchRequest = mockRestServer.configureModelPostRequest(MockRestServer.ENCRYPTED_PROTOCOL).build();
        // accessing an undefined field in a key with a specific source match fails with a key error
        testDomain(anyMatchRequest, Lists.newArrayList("Variable(s) 'domain' required by rest entity are undefined"));
    }

    private void testDomain(RestEnrichmentConfig config, String domainName, Boolean legit) throws Exception {
        Map<String, String> variables = new HashMap<String, String>() {{
            put(MockRestServer.DOMAIN_EXTENSION_NAME, domainName);
        }};
        RestRequestResult result = makeRequest(config, variables);
        Assert.assertEquals(legit.toString(), result.getExtensions().get(MockRestServer.LEGIT_RESPONSE));
        Assert.assertTrue(result.getErrors().isEmpty());
    }

    private void testDomain(RestEnrichmentConfig config, List<String> expectedErrors) throws Exception {
        Map<String, String> variables = new HashMap<String, String>() {{
            put("wrong field name", "testdomain");
        }};
        RestRequestResult result = makeRequest(config, variables);
        Assert.assertNull(result.getExtensions().get(MockRestServer.LEGIT_RESPONSE));
        Assert.assertEquals(expectedErrors, result.getErrors());
    }

}
