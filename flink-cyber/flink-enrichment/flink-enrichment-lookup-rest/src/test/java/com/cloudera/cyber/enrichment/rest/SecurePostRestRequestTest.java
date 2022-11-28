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
import org.hamcrest.Matcher;
import org.junit.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class SecurePostRestRequestTest extends PostRestRequestTest {

    private static final String KEY_ALIAS = "client";

    @BeforeClass
    public static void createMockService() {
        createMockService(true);
    }

    @AfterClass
    public static void stopMockServer() {
        mockRestServer.close();
    }

    @Test
    public void testTlsWithAlias() throws Exception {
        RestEnrichmentConfig.RestEnrichmentConfigBuilder modelResultPostRequestWithKeyAlias = mockRestServer.configureTLS(mockRestServer.configureModelPostRequest(), KEY_ALIAS);
        testSuccessfulModelPosts(modelResultPostRequestWithKeyAlias.build());
    }

    @Test
    public void testTlsHandshakeException() throws Exception {
        RestEnrichmentConfig.RestEnrichmentConfigBuilder modelResultPostRequestWithKeyAlias = mockRestServer.configureTLS(mockRestServer.configureModelPostRequest(), "nosuchkeyalias");
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

}